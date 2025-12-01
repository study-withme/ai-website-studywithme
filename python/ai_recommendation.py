#!/usr/bin/env python3
"""
AI 기반 사용자 맞춤형 게시글 추천 시스템
사용자 활동 로그를 분석하여 개인화된 추천을 제공합니다.

알고리즘:
1. 협업 필터링 (Collaborative Filtering)
   - User-based CF: 비슷한 사용자들이 좋아한 게시글 추천
   - Item-based CF: 비슷한 게시글 추천
2. 콘텐츠 기반 필터링 (Content-based Filtering)
3. 하이브리드 추천 (Hybrid Recommendation)
"""

import json
import sys
import mysql.connector
import math
from collections import defaultdict, Counter
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Optional, Set
from config import Config
from logger import setup_logger

logger = setup_logger(__name__)


class CollaborativeFiltering:
    """협업 필터링 알고리즘"""
    
    def __init__(self, user_item_matrix: Dict[int, Dict[int, float]]):
        """
        Args:
            user_item_matrix: {user_id: {item_id: rating}}
        """
        self.user_item_matrix = user_item_matrix
        self.item_user_matrix = self._build_item_user_matrix()
    
    def _build_item_user_matrix(self) -> Dict[int, Dict[int, float]]:
        """아이템-사용자 행렬 구축"""
        item_user = defaultdict(dict)
        for user_id, items in self.user_item_matrix.items():
            for item_id, rating in items.items():
                item_user[item_id][user_id] = rating
        return dict(item_user)
    
    def cosine_similarity(self, vec1: Dict[int, float], vec2: Dict[int, float]) -> float:
        """코사인 유사도 계산"""
        # 공통 아이템 찾기
        common_items = set(vec1.keys()) & set(vec2.keys())
        if not common_items:
            return 0.0
        
        # 내적 계산
        dot_product = sum(vec1[item] * vec2[item] for item in common_items)
        
        # 벡터 크기 계산
        magnitude1 = math.sqrt(sum(v ** 2 for v in vec1.values()))
        magnitude2 = math.sqrt(sum(v ** 2 for v in vec2.values()))
        
        if magnitude1 == 0 or magnitude2 == 0:
            return 0.0
        
        return dot_product / (magnitude1 * magnitude2)
    
    def pearson_correlation(self, vec1: Dict[int, float], vec2: Dict[int, float]) -> float:
        """피어슨 상관계수 계산"""
        common_items = list(set(vec1.keys()) & set(vec2.keys()))
        if len(common_items) < 2:
            return 0.0
        
        # 평균 계산
        mean1 = sum(vec1[item] for item in common_items) / len(common_items)
        mean2 = sum(vec2[item] for item in common_items) / len(common_items)
        
        # 분자: 공분산
        numerator = sum((vec1[item] - mean1) * (vec2[item] - mean2) for item in common_items)
        
        # 분모: 표준편차
        sum_sq1 = sum((vec1[item] - mean1) ** 2 for item in common_items)
        sum_sq2 = sum((vec2[item] - mean2) ** 2 for item in common_items)
        
        if sum_sq1 == 0 or sum_sq2 == 0:
            return 0.0
        
        denominator = math.sqrt(sum_sq1 * sum_sq2)
        return numerator / denominator if denominator != 0 else 0.0
    
    def find_similar_users(self, target_user_id: int, n: int = 10, 
                          similarity_func: str = 'cosine') -> List[Tuple[int, float]]:
        """비슷한 사용자 찾기 (User-based CF)"""
        if target_user_id not in self.user_item_matrix:
            return []
        
        target_vector = self.user_item_matrix[target_user_id]
        similarities = []
        
        similarity_fn = self.cosine_similarity if similarity_func == 'cosine' else self.pearson_correlation
        
        for user_id, user_vector in self.user_item_matrix.items():
            if user_id == target_user_id:
                continue
            
            similarity = similarity_fn(target_vector, user_vector)
            if similarity > 0:
                similarities.append((user_id, similarity))
        
        # 유사도 순으로 정렬
        similarities.sort(key=lambda x: x[1], reverse=True)
        return similarities[:n]
    
    def find_similar_items(self, target_item_id: int, n: int = 10,
                          similarity_func: str = 'cosine') -> List[Tuple[int, float]]:
        """비슷한 아이템 찾기 (Item-based CF)"""
        if target_item_id not in self.item_user_matrix:
            return []
        
        target_vector = self.item_user_matrix[target_item_id]
        similarities = []
        
        similarity_fn = self.cosine_similarity if similarity_func == 'cosine' else self.pearson_correlation
        
        for item_id, item_vector in self.item_user_matrix.items():
            if item_id == target_item_id:
                continue
            
            similarity = similarity_fn(target_vector, item_vector)
            if similarity > 0:
                similarities.append((item_id, similarity))
        
        similarities.sort(key=lambda x: x[1], reverse=True)
        return similarities[:n]
    
    def user_based_recommend(self, target_user_id: int, n: int = 20,
                            min_similarity: float = 0.1) -> List[Tuple[int, float]]:
        """User-based 협업 필터링 추천"""
        if target_user_id not in self.user_item_matrix:
            return []
        
        target_items = set(self.user_item_matrix[target_user_id].keys())
        similar_users = self.find_similar_users(target_user_id, n=50)
        
        # 예상 평점 계산
        item_scores = defaultdict(lambda: {'weighted_sum': 0.0, 'similarity_sum': 0.0})
        
        for similar_user_id, similarity in similar_users:
            if similarity < min_similarity:
                continue
            
            similar_user_items = self.user_item_matrix[similar_user_id]
            for item_id, rating in similar_user_items.items():
                if item_id not in target_items:  # 아직 평가하지 않은 아이템만
                    item_scores[item_id]['weighted_sum'] += similarity * rating
                    item_scores[item_id]['similarity_sum'] += abs(similarity)
        
        # 예상 평점 계산
        recommendations = []
        for item_id, scores in item_scores.items():
            if scores['similarity_sum'] > 0:
                predicted_rating = scores['weighted_sum'] / scores['similarity_sum']
                recommendations.append((item_id, predicted_rating))
        
        recommendations.sort(key=lambda x: x[1], reverse=True)
        return recommendations[:n]
    
    def item_based_recommend(self, target_user_id: int, n: int = 20,
                            min_similarity: float = 0.1) -> List[Tuple[int, float]]:
        """Item-based 협업 필터링 추천"""
        if target_user_id not in self.user_item_matrix:
            return []
        
        target_user_items = self.user_item_matrix[target_user_id]
        item_scores = defaultdict(float)
        item_weights = defaultdict(float)
        
        # 사용자가 평가한 각 아이템에 대해
        for rated_item_id, rating in target_user_items.items():
            # 비슷한 아이템 찾기
            similar_items = self.find_similar_items(rated_item_id, n=20)
            
            for similar_item_id, similarity in similar_items:
                if similar_item_id in target_user_items:
                    continue  # 이미 평가한 아이템은 제외
                
                if similarity >= min_similarity:
                    item_scores[similar_item_id] += similarity * rating
                    item_weights[similar_item_id] += abs(similarity)
        
        # 예상 평점 계산
        recommendations = []
        for item_id in item_scores:
            if item_weights[item_id] > 0:
                predicted_rating = item_scores[item_id] / item_weights[item_id]
                recommendations.append((item_id, predicted_rating))
        
        recommendations.sort(key=lambda x: x[1], reverse=True)
        return recommendations[:n]


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
    
    def get_user_preferences(self, user_id: int) -> List[Dict]:
        """사용자가 직접 선택한 카테고리 선호도 조회"""
        cursor = self.conn.cursor(dictionary=True)
        
        query = """
            SELECT 
                category_name,
                preference_score,
                created_at
            FROM user_preferences
            WHERE user_id = %s
            ORDER BY preference_score DESC, created_at DESC
        """
        
        cursor.execute(query, (user_id,))
        results = cursor.fetchall()
        cursor.close()
        
        return results
    
    def analyze_user_preferences(self, user_id: int) -> Dict:
        """사용자 선호도 분석"""
        activities = self.get_user_activities(user_id)
        
        # 사용자가 직접 선택한 카테고리 선호도 조회
        user_prefs = self.get_user_preferences(user_id)
        
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
        
        # 카테고리 매핑 (실제 DB 카테고리: 개발, 자격증, 영어, 독서, 취업, 기타)
        category_mapping = {
            '프로그래밍': '개발',
            '언어': '영어',
            '코딩': '개발'
        }
        
        # 사용자가 직접 선택한 카테고리에 높은 가중치 부여
        # 단, "고정 프로필"이 행동 로그를 완전히 덮어쓰지 않도록 조정
        # - 활동 로그가 거의 없을 때: 프로필 가중치 ↑
        # - 활동 로그가 충분히 쌓였을 때: 행동 기반 가중치가 더 크게 작용
        for pref in user_prefs:
            category_name = pref['category_name']
            mapped_category = category_mapping.get(category_name, category_name)
            preference_score = pref['preference_score'] or 1.0
            category_scores[mapped_category] += 5.0 * preference_score
            logger.info(f"사용자 선택 카테고리 반영: {category_name} -> {mapped_category} (점수: {5.0 * preference_score})")
        
        # 활동 로그 기반 선호도 계산 (검색/클릭/좋아요/북마크/댓글/AI_CLICK/RECOMMEND)
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
        
        # 활동 로그 기반 카테고리 점수만 정규화
        activity_based_scores = {}
        for cat, score in category_scores.items():
            # 사용자 선택 카테고리인지 확인
            is_user_selected = any(pref['category_name'] == cat for pref in user_prefs)
            if is_user_selected:
                # 사용자 선택은 정규화하지 않음 (원래 점수 유지)
                activity_based_scores[cat] = score
            else:
                # 활동 로그 기반은 정규화
                activity_based_scores[cat] = score / total_weight if total_weight > 0 else 0
        
        normalized_categories = activity_based_scores
        
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
            'total_activities': len(activities),
            'user_selected_categories': [pref['category_name'] for pref in user_prefs]
        }
    
    def build_user_item_matrix(self, days: int = 90) -> Dict[int, Dict[int, float]]:
        """사용자-아이템 행렬 구축 (협업 필터링용)"""
        cursor = self.conn.cursor(dictionary=True)
        
        # 액션 타입별 가중치
        action_weights = {
            'SEARCH': 1.0,
            'CLICK': 2.0,
            'LIKE': 3.0,
            'BOOKMARK': 4.0,
            'COMMENT': 3.5,
            'AI_CLICK': 5.0,
            'RECOMMEND': 2.5
        }
        
        query = """
            SELECT 
                ua.user_id,
                ua.target_id as post_id,
                ua.action_type,
                COUNT(*) as action_count
            FROM user_activity ua
            WHERE ua.target_id IS NOT NULL
              AND ua.created_at >= DATE_SUB(NOW(), INTERVAL %s DAY)
            GROUP BY ua.user_id, ua.target_id, ua.action_type
        """
        
        cursor.execute(query, (days,))
        results = cursor.fetchall()
        cursor.close()
        
        # 좋아요 데이터 추가
        cursor = self.conn.cursor(dictionary=True)
        like_query = """
            SELECT user_id, post_id, COUNT(*) as like_count
            FROM post_likes
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL %s DAY)
            GROUP BY user_id, post_id
        """
        cursor.execute(like_query, (days,))
        likes = cursor.fetchall()
        cursor.close()
        
        # 북마크 데이터 추가
        cursor = self.conn.cursor(dictionary=True)
        bookmark_query = """
            SELECT user_id, post_id, COUNT(*) as bookmark_count
            FROM bookmarks
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL %s DAY)
            GROUP BY user_id, post_id
        """
        cursor.execute(bookmark_query, (days,))
        bookmarks = cursor.fetchall()
        cursor.close()
        
        # 사용자-아이템 행렬 구축
        user_item_matrix = defaultdict(lambda: defaultdict(float))
        
        # 활동 로그 기반 점수
        for row in results:
            user_id = row['user_id']
            post_id = row['post_id']
            action_type = row['action_type']
            count = row['action_count']
            
            weight = action_weights.get(action_type, 1.0)
            user_item_matrix[user_id][post_id] += weight * count
        
        # 좋아요 기반 점수
        for row in likes:
            user_id = row['user_id']
            post_id = row['post_id']
            count = row['like_count']
            user_item_matrix[user_id][post_id] += 3.0 * count
        
        # 북마크 기반 점수
        for row in bookmarks:
            user_id = row['user_id']
            post_id = row['post_id']
            count = row['bookmark_count']
            user_item_matrix[user_id][post_id] += 4.0 * count
        
        # 정규화 (0-5 스케일로)
        max_rating = 5.0
        for user_id in user_item_matrix:
            max_score = max(user_item_matrix[user_id].values()) if user_item_matrix[user_id] else 1.0
            if max_score > 0:
                for post_id in user_item_matrix[user_id]:
                    user_item_matrix[user_id][post_id] = min(
                        (user_item_matrix[user_id][post_id] / max_score) * max_rating,
                        max_rating
                    )
        
        return dict(user_item_matrix)
    
    def get_recommended_posts(self, user_id: int, limit: int = 20, 
                             use_collaborative_filtering: bool = True) -> List[Dict]:
        """사용자에게 추천할 게시글 조회 (하이브리드 추천)"""
        preferences = self.analyze_user_preferences(user_id)
        
        # 사용자가 선호하는 카테고리 집합 (DB 카테고리 기준: 개발, 자격증, 영어, 독서, 취업, 기타)
        preferred_categories: Set[str] = set(preferences.get('categories', {}).keys())
        
        # 협업 필터링 추천
        cf_recommendations = []
        if use_collaborative_filtering:
            try:
                user_item_matrix = self.build_user_item_matrix()
                if user_id in user_item_matrix and len(user_item_matrix) > 1:
                    cf = CollaborativeFiltering(user_item_matrix)
                    
                    # User-based CF
                    user_based = cf.user_based_recommend(user_id, n=limit)
                    
                    # Item-based CF
                    item_based = cf.item_based_recommend(user_id, n=limit)
                    
                    # 두 결과 결합 (가중 평균)
                    combined_scores = defaultdict(lambda: {'score': 0.0, 'count': 0})
                    
                    for post_id, score in user_based:
                        combined_scores[post_id]['score'] += score * 0.6  # User-based 가중치
                        combined_scores[post_id]['count'] += 1
                    
                    for post_id, score in item_based:
                        combined_scores[post_id]['score'] += score * 0.4  # Item-based 가중치
                        combined_scores[post_id]['count'] += 1
                    
                    # 평균 점수 계산
                    cf_recommendations = [
                        (post_id, scores['score'] / scores['count'] if scores['count'] > 0 else scores['score'])
                        for post_id, scores in combined_scores.items()
                    ]
                    cf_recommendations.sort(key=lambda x: x[1], reverse=True)
                    cf_recommendations = cf_recommendations[:limit]
                    
                    logger.info(f"협업 필터링 추천: {len(cf_recommendations)}개 게시글")
            except Exception as e:
                logger.warning(f"협업 필터링 실패, 콘텐츠 기반으로 폴백: {e}")
        
        # 콘텐츠 기반 필터링 (기존 방식)
        if not preferences['categories'] and not preferences['tags']:
            # 선호도가 없으면 최신 게시글 반환
            content_based = self.get_recent_posts(limit)
        else:
            content_based = self._get_content_based_recommendations(user_id, preferences, limit)
        
        # 하이브리드 추천: 협업 필터링 + 콘텐츠 기반 결합
        if cf_recommendations:
            # 협업 필터링 결과와 콘텐츠 기반 결과 결합
            cf_post_ids = {post_id for post_id, _ in cf_recommendations}
            content_post_ids = {post['id'] for post in content_based}
            
            # 점수 정규화 및 결합
            final_scores = {}
            
            # 협업 필터링 점수 (0-100 스케일)
            max_cf_score = max(score for _, score in cf_recommendations) if cf_recommendations else 1.0
            for post_id, score in cf_recommendations:
                normalized_score = (score / max_cf_score) * 100 if max_cf_score > 0 else 0
                final_scores[post_id] = normalized_score * 0.6  # 60% 가중치
            
            # 콘텐츠 기반 점수
            for post in content_based:
                post_id = post['id']
                content_score = post.get('recommendation_score', 0)
                if post_id in final_scores:
                    final_scores[post_id] += content_score * 0.4  # 40% 가중치
                else:
                    final_scores[post_id] = content_score * 0.4
            
            # 모든 게시글 ID 수집
            all_post_ids = set(final_scores.keys())
            
            # 게시글 정보 조회 (선호 카테고리에 속한 게시글만 남기기)
            recommended_posts = self._get_posts_by_ids(list(all_post_ids), limit, preferred_categories)
            
            # 최종 점수 적용
            for post in recommended_posts:
                post_id = post['id']
                post['recommendation_score'] = round(final_scores.get(post_id, 0), 2)
                post['cf_score'] = round((post['recommendation_score'] / 0.6) if post_id in cf_post_ids else 0, 2)
                post['content_score'] = round((post['recommendation_score'] / 0.4) if post_id in content_post_ids else 0, 2)
            
            # 점수 기준 상위 후보군(top_k)을 만든 뒤, 그 안에서 약간의 랜덤성을 주어
            # 매 새로고침마다 구성이 조금씩 달라지도록 함
            recommended_posts.sort(key=lambda x: x['recommendation_score'], reverse=True)
            top_k = recommended_posts[:max(limit * 2, limit)]
            
            import random
            random.shuffle(top_k)
            return top_k[:limit]
        else:
            # 협업 필터링 결과가 없으면 콘텐츠 기반만 사용
            return content_based
    
    def _get_content_based_recommendations(self, user_id: int, preferences: Dict, limit: int) -> List[Dict]:
        """콘텐츠 기반 추천 (기존 로직)"""
        cursor = self.conn.cursor(dictionary=True)
        
        # 카테고리 매핑
        category_mapping = {
            '프로그래밍': '개발',
            '언어': '영어',
            '코딩': '개발'
        }
        
        raw_categories = list(preferences['categories'].keys())[:5]
        categories = [category_mapping.get(cat, cat) for cat in raw_categories]
        categories = [cat for cat in dict.fromkeys(categories) if cat]
        tags = list(preferences['tags'].keys())[:10]
        
        # 선호 카테고리/태그가 전혀 없으면 최신 글로 폴백
        if not categories and not tags:
            return self.get_recent_posts(limit)
        
        # 태그 조건
        tag_conditions = " OR ".join([f"p.tags LIKE %s" for _ in tags])
        
        # 사용자가 실제로 선호/활동한 카테고리에 속한 글만 추천하도록 강하게 제한
        # - p.category는 반드시 선호 카테고리 중 하나여야 함
        # - 태그는 점수 계산 및 추가 필터링에만 사용
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
                    WHEN ({tag_conditions if tags else 'FALSE'}) THEN 1 ELSE 0 
                END as tag_match
            FROM posts p
            WHERE p.id NOT IN (
                SELECT DISTINCT target_id 
                FROM user_activity 
                WHERE user_id = %s AND target_id IS NOT NULL
            )
            AND p.category IN ({','.join(['%s'] * len(categories))})
            AND ({tag_conditions if tags else 'TRUE'})
            ORDER BY 
                category_match DESC,
                tag_match DESC,
                (p.like_count * 2 + p.view_count) DESC,
                p.created_at DESC
            LIMIT %s
        """
        
        # 파라미터 순서:
        # 1) category_match IN (...) 용 카테고리 목록
        # 2) tag_match 조건 / 태그 필터용 LIKE 파라미터
        # 3) 이미 본 글 제외를 위한 user_id
        # 4) WHERE p.category IN (...) 필터용 카테고리 목록
        # 5) AND ({tag_conditions}) 필터용 태그 LIKE 파라미터
        params = []
        # 1) category_match IN (...)
        params.extend(categories)
        # 2) tag_match용 태그
        if tags:
            params.extend([f"%{tag}%" for tag in tags])
        # 3) user_id
        params.append(user_id)
        # 4) WHERE p.category IN (...)
        params.extend(categories)
        # 5) 본문 필터용 태그 LIKE
        if tags:
            params.extend([f"%{tag}%" for tag in tags])
        params.append(limit)
        
        cursor.execute(query, params)
        results = cursor.fetchall()
        cursor.close()
        
        # 점수 계산
        scored_posts = []
        for post in results:
            score = 0
            
            post_category = post['category']
            mapped_category = category_mapping.get(post_category, post_category)
            
            if post_category in preferences['categories']:
                score += preferences['categories'][post_category] * 100
            elif mapped_category in preferences['categories']:
                score += preferences['categories'][mapped_category] * 100
            
            if post['tags']:
                post_tags = [t.strip() for t in post['tags'].split(',') if t.strip()]
                for tag in post_tags:
                    if tag in preferences['tags']:
                        score += preferences['tags'][tag] * 50
            
            score += (post['like_count'] or 0) * 2
            score += (post['view_count'] or 0) * 0.1
            
            days_old = (datetime.now() - post['created_at']).days
            if days_old <= 7:
                score += 10
            
            post['recommendation_score'] = round(score, 2)
            scored_posts.append(post)
        
        # 점수 상위권에서만 랜덤하게 뽑아서 "항상 똑같은 글"이 고정되지 않도록 조정
        scored_posts.sort(key=lambda x: x['recommendation_score'], reverse=True)
        top_k = scored_posts[:max(limit * 2, limit)]
        
        import random
        random.shuffle(top_k)
        return top_k[:limit]
    
    def _get_posts_by_ids(self, post_ids: List[int], limit: int, preferred_categories: Optional[Set[str]] = None) -> List[Dict]:
        """게시글 ID 리스트로 게시글 조회
        
        preferred_categories가 주어지면, 해당 카테고리에 속한 게시글만 반환합니다.
        """
        if not post_ids:
            return []
        
        cursor = self.conn.cursor(dictionary=True)
        placeholders = ','.join(['%s'] * len(post_ids))
        query = f"""
            SELECT 
                p.id,
                p.title,
                p.category,
                p.tags,
                p.view_count,
                p.like_count,
                p.created_at
            FROM posts p
            WHERE p.id IN ({placeholders})
            ORDER BY p.created_at DESC
            LIMIT %s
        """
        cursor.execute(query, post_ids + [limit])
        results = cursor.fetchall()
        cursor.close()
        
        # 선호 카테고리가 있다면 그 안에 속한 게시글만 남김
        if preferred_categories:
            filtered = [r for r in results if r.get('category') in preferred_categories]
            # 필터 결과가 비면 원본 결과를 사용 (너무 빡세게 걸러지지 않도록)
            return filtered or results
        
        return results
    
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

