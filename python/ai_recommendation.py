#!/usr/bin/env python3
"""
AI 기반 사용자 맞춤형 게시글 추천 시스템
사용자 활동 로그를 분석하여 개인화된 추천을 제공합니다.
"""

import json
import sys
import mysql.connector
from collections import defaultdict, Counter
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Optional
from config import Config
from logger import setup_logger

logger = setup_logger(__name__)


class UserActivityAnalyzer:
    """사용자 활동 로그 분석기"""
    
    def __init__(self, db_config: Optional[Dict] = None):
        self.db_config = db_config or Config.get_db_config()
        self.conn = None
    
    def connect(self):
        """데이터베이스 연결"""
        try:
            self.conn = mysql.connector.connect(**self.db_config)
            logger.info(f"데이터베이스 연결 성공: {self.db_config['host']}:{self.db_config['port']}/{self.db_config['database']}")
            return True
        except mysql.connector.Error as e:
            logger.error(f"데이터베이스 연결 실패: {e}", exc_info=True)
            print(f"데이터베이스 연결 실패: {e}", file=sys.stderr)
            return False
    
    def close(self):
        """데이터베이스 연결 종료"""
        if self.conn and self.conn.is_connected():
            self.conn.close()
    
    def get_user_activities(self, user_id: int, days: int = 30) -> List[Dict]:
        """사용자의 최근 활동 로그 조회"""
        cursor = self.conn.cursor(dictionary=True)
        
        query = """
            SELECT 
                ua.action_type,
                ua.target_id,
                ua.target_keyword,
                ua.action_detail,
                ua.created_at,
                p.category,
                p.tags,
                p.title
            FROM user_activity ua
            LEFT JOIN posts p ON ua.target_id = p.id
            WHERE ua.user_id = %s
              AND ua.created_at >= DATE_SUB(NOW(), INTERVAL %s DAY)
            ORDER BY ua.created_at DESC
        """
        
        cursor.execute(query, (user_id, days))
        results = cursor.fetchall()
        cursor.close()
        
        return results
    
    def analyze_user_preferences(self, user_id: int) -> Dict:
        """사용자 선호도 분석"""
        activities = self.get_user_activities(user_id)
        
        if not activities:
            return {
                'categories': {},
                'tags': {},
                'action_weights': {},
                'total_activities': 0
            }
        
        # 카테고리별 가중치 계산
        category_scores = defaultdict(float)
        tag_scores = defaultdict(float)
        action_counts = Counter()
        
        # 액션 타입별 가중치
        action_weights = {
            'SEARCH': 1.0,
            'CLICK': 2.0,
            'LIKE': 3.0,
            'BOOKMARK': 4.0,
            'COMMENT': 3.5,
            'AI_CLICK': 5.0,  # AI 버튼 클릭은 높은 가중치
            'RECOMMEND': 2.5
        }
        
        for activity in activities:
            action_type = activity['action_type']
            action_counts[action_type] += 1
            
            weight = action_weights.get(action_type, 1.0)
            
            # 카테고리 점수
            if activity['category']:
                category_scores[activity['category']] += weight
            
            # 태그 점수
            if activity['tags']:
                tags = [t.strip() for t in activity['tags'].split(',') if t.strip()]
                for tag in tags:
                    tag_scores[tag] += weight
            
            # 검색 키워드도 태그로 간주
            if activity['target_keyword'] and action_type == 'SEARCH':
                keyword = activity['target_keyword'].strip()
                tag_scores[keyword] += weight * 0.8
        
        # 정규화 (총 활동 수로 나누기)
        total_weight = sum(action_weights.get(a, 1.0) * c for a, c in action_counts.items())
        
        normalized_categories = {
            cat: score / total_weight if total_weight > 0 else 0
            for cat, score in category_scores.items()
        }
        
        normalized_tags = {
            tag: score / total_weight if total_weight > 0 else 0
            for tag, score in tag_scores.items()
        }
        
        return {
            'categories': dict(sorted(normalized_categories.items(), 
                                    key=lambda x: x[1], reverse=True)[:10]),
            'tags': dict(sorted(normalized_tags.items(), 
                              key=lambda x: x[1], reverse=True)[:20]),
            'action_counts': dict(action_counts),
            'total_activities': len(activities)
        }
    
    def get_recommended_posts(self, user_id: int, limit: int = 20) -> List[Dict]:
        """사용자에게 추천할 게시글 조회"""
        preferences = self.analyze_user_preferences(user_id)
        
        if not preferences['categories'] and not preferences['tags']:
            # 선호도가 없으면 최신 게시글 반환
            return self.get_recent_posts(limit)
        
        cursor = self.conn.cursor(dictionary=True)
        
        # 카테고리와 태그 기반으로 게시글 검색
        categories = list(preferences['categories'].keys())[:5]
        tags = list(preferences['tags'].keys())[:10]
        
        # SQL 쿼리 생성
        category_conditions = " OR ".join([f"p.category = %s" for _ in categories])
        tag_conditions = " OR ".join([f"p.tags LIKE %s" for _ in tags])
        
        query = f"""
            SELECT 
                p.id,
                p.title,
                p.category,
                p.tags,
                p.view_count,
                p.like_count,
                p.created_at,
                CASE 
                    WHEN p.category IN ({','.join(['%s'] * len(categories))}) THEN 1 ELSE 0 
                END as category_match,
                CASE 
                    WHEN ({' OR '.join([f"p.tags LIKE %s" for _ in tags])}) THEN 1 ELSE 0 
                END as tag_match
            FROM posts p
            WHERE p.id NOT IN (
                SELECT DISTINCT target_id 
                FROM user_activity 
                WHERE user_id = %s AND target_id IS NOT NULL
            )
            AND (
                {category_conditions if categories else 'FALSE'}
                OR {tag_conditions if tags else 'FALSE'}
            )
            ORDER BY 
                category_match DESC,
                tag_match DESC,
                (p.like_count * 2 + p.view_count) DESC,
                p.created_at DESC
            LIMIT %s
        """
        
        params = []
        if categories:
            params.extend(categories)
        if tags:
            params.extend([f"%{tag}%" for tag in tags])
        params.append(user_id)
        params.extend(categories)
        params.extend([f"%{tag}%" for tag in tags])
        params.append(limit)
        
        cursor.execute(query, params)
        results = cursor.fetchall()
        cursor.close()
        
        # 점수 계산
        scored_posts = []
        for post in results:
            score = 0
            
            # 카테고리 매칭 점수
            if post['category'] in preferences['categories']:
                score += preferences['categories'][post['category']] * 100
            
            # 태그 매칭 점수
            if post['tags']:
                post_tags = [t.strip() for t in post['tags'].split(',') if t.strip()]
                for tag in post_tags:
                    if tag in preferences['tags']:
                        score += preferences['tags'][tag] * 50
            
            # 인기도 점수
            score += (post['like_count'] or 0) * 2
            score += (post['view_count'] or 0) * 0.1
            
            # 최신성 점수 (최근 7일 내면 보너스)
            days_old = (datetime.now() - post['created_at']).days
            if days_old <= 7:
                score += 10
            
            post['recommendation_score'] = round(score, 2)
            scored_posts.append(post)
        
        # 점수 순으로 정렬
        scored_posts.sort(key=lambda x: x['recommendation_score'], reverse=True)
        
        return scored_posts[:limit]
    
    def get_recent_posts(self, limit: int = 20) -> List[Dict]:
        """최신 게시글 조회"""
        cursor = self.conn.cursor(dictionary=True)
        query = """
            SELECT 
                p.id,
                p.title,
                p.category,
                p.tags,
                p.view_count,
                p.like_count,
                p.created_at
            FROM posts p
            ORDER BY p.created_at DESC
            LIMIT %s
        """
        cursor.execute(query, (limit,))
        results = cursor.fetchall()
        cursor.close()
        return results


def main():
    """메인 함수"""
    if len(sys.argv) < 2:
        print(json.dumps({
            'error': '사용자 ID가 필요합니다.',
            'usage': 'python ai_recommendation.py <user_id> [limit]'
        }), file=sys.stderr)
        sys.exit(1)
    
    try:
        user_id = int(sys.argv[1])
        limit = int(sys.argv[2]) if len(sys.argv) > 2 else Config.DEFAULT_RECOMMENDATION_LIMIT
    except ValueError:
        print(json.dumps({
            'error': '잘못된 인자입니다. user_id와 limit는 정수여야 합니다.'
        }), file=sys.stderr)
        sys.exit(1)
    
    # Config 클래스가 이미 환경 변수를 읽으므로 추가 설정 불필요
    analyzer = UserActivityAnalyzer()
    
    if not analyzer.connect():
        sys.exit(1)
    
    try:
        # 사용자 선호도 분석
        logger.info(f"사용자 {user_id}의 선호도 분석 시작")
        preferences = analyzer.analyze_user_preferences(user_id)
        
        # 추천 게시글 조회
        logger.info(f"사용자 {user_id}에게 {limit}개의 게시글 추천 시작")
        recommended_posts = analyzer.get_recommended_posts(user_id, limit)
        
        # 결과를 JSON으로 출력
        result = {
            'user_id': user_id,
            'preferences': preferences,
            'recommended_posts': [
                {
                    'id': post['id'],
                    'title': post['title'],
                    'category': post['category'],
                    'tags': post['tags'],
                    'view_count': post['view_count'],
                    'like_count': post['like_count'],
                    'recommendation_score': post.get('recommendation_score', 0),
                    'created_at': post['created_at'].isoformat() if post['created_at'] else None
                }
                for post in recommended_posts
            ],
            'total_recommended': len(recommended_posts)
        }
        
        logger.info(f"추천 완료: {len(recommended_posts)}개의 게시글 추천됨")
        print(json.dumps(result, ensure_ascii=False, indent=2))
        
    except Exception as e:
        logger.error(f"추천 시스템 오류: {e}", exc_info=True)
        print(json.dumps({
            'error': str(e)
        }), file=sys.stderr)
        sys.exit(1)
    finally:
        analyzer.close()


if __name__ == '__main__':
    main()

