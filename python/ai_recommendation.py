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
import re
from collections import defaultdict, Counter
from datetime import datetime, timedelta
from typing import Dict, List, Tuple, Optional, Set
from config import Config
from logger import setup_logger

logger = setup_logger(__name__)

# 카테고리별 키워드 사전 (DB 카테고리 기준: 개발, 자격증, 영어, 독서, 취업, 기타)
CATEGORY_KEYWORDS = {
    '개발': [
        'java', 'python', 'javascript', 'typescript', 'react', 'vue', 'angular',
        'spring', 'django', 'flask', 'node.js', 'express', 'mysql', 'postgresql',
        'mongodb', 'redis', 'docker', 'kubernetes', 'aws', 'git', 'github',
        'html', 'css', 'scss', 'bootstrap', 'tailwind', 'jquery', 'rest api',
        'graphql', 'jpa', 'hibernate', 'mybatis', 'spring boot', 'spring security',
        '코딩', '프로그래밍', '개발', '소프트웨어', '알고리즘', '자바', '파이썬', 
        '자바스크립트', '스프링', '리액트', '앱', '웹', '백엔드', '프론트엔드', 
        '데이터베이스', 'api', '프레임워크', '개발자', 'it서적', 'it 서적',
        '개발자 취업', 'it 취업', '개발 취업', '프로그래머', '소프트웨어 개발자',
        'it', 'it기업', 'it 기업', '스타트업', 'startup', '테크', 'tech'
    ],
    '영어': [
        '영어', 'english', '토익', 'toeic', '토플', 'toefl', 'ielts', 'teps',
        '회화', 'conversation', 'speaking', 'listening', 'reading', 'writing',
        '문법', 'grammar', 'vocabulary', '단어', '어휘', '발음', 'pronunciation',
        '영어책', '영문', '영어공부', '영어학습', '영어회화', '영어독해'
    ],
    '독서': [
        '독서', 'reading', '책', 'book', '북클럽', 'bookclub', '독후감',
        '서평', '리뷰', 'review', '문학', '소설', '에세이', '인문학',
        '철학', '역사', '경제', '경영', '자기계발', '인문', '고전'
    ],
    '자격증': [
        '자격증', 'certificate', 'license', '시험', 'exam', 'test',
        '합격', 'pass', '공인', '인증', '자격', '면접', 'interview',
        '정보처리기사', '컴활', '토익', '토플', '한국사', '공인회계사',
        '변호사', '의사', '간호사', '교사', '공무원'
    ],
    '취업': [
        '취업', 'job', 'employment', '면접', 'interview', '포트폴리오', 'portfolio',
        '이력서', 'resume', '자소서', '자기소개서', '인턴', 'intern', '신입', 'newbie',
        '경력', 'career', '채용', 'recruitment', '공채', '사채', '스펙', 'spec',
        '개발자 취업', 'it 취업', '개발 취업', '취업 준비', '취업 스터디', '취업 토론',
        '개발자 면접', 'it 면접', '기술 면접', '코딩 테스트', '코테'
    ],
    '기타': [
        '스터디', 'study', '모임', 'meeting', '친목', '네트워킹', 'networking',
        '소통', 'communication', '커뮤니티', 'community', '동아리', 'club'
    ]
}


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
                p.title,
                p.content
            FROM user_activity ua
            LEFT JOIN posts p ON ua.target_id = p.id
            WHERE ua.user_id = %s
              AND ua.created_at >= DATE_SUB(NOW(), INTERVAL %s DAY)
            ORDER BY ua.created_at DESC
        """
        
        cursor.execute(query, (user_id, days))
        results = cursor.fetchall()
        cursor.close()
        
        # 디버깅: 활동 로그 상세 정보 출력
        logger.info(f"📊 사용자 {user_id}의 최근 {days}일 활동 로그: {len(results)}개")
        category_counts = Counter()
        category_null_count = 0
        
        for activity in results:
            category = activity.get('category')
            action_type = activity.get('action_type')
            target_id = activity.get('target_id')
            # title이 None일 수 있으므로 안전하게 처리
            raw_title = activity.get('title') or 'N/A'
            title = raw_title[:50]
            
            if category:
                category_counts[category] += 1
                logger.debug(f"  ✅ {action_type}: 카테고리={category}, 제목={title}")
            else:
                category_null_count += 1
                logger.warning(f"  ⚠️ {action_type}: 카테고리=NULL, target_id={target_id}, 제목={title}")
        
        if category_counts:
            logger.info(f"✅ 카테고리별 활동 횟수: {dict(category_counts)}")
        else:
            logger.warning(f"❌ 활동 로그에 카테고리 정보가 없습니다! (NULL: {category_null_count}개)")
            logger.warning(f"⚠️ 게시글 클릭 시 카테고리가 제대로 저장되는지 확인하세요.")
        
        return results
    
    def get_user_viewed_posts(self, user_id: int, days: int = 30) -> List[Dict]:
        """사용자가 실제로 본 게시글들의 상세 정보 조회 (클릭/좋아요/북마크)"""
        cursor = self.conn.cursor(dictionary=True)
        
        query = """
            SELECT DISTINCT
                p.id,
                p.title,
                p.content,
                p.category,
                p.tags,
                p.view_count,
                p.like_count,
                ua.action_type,
                ua.created_at
            FROM user_activity ua
            INNER JOIN posts p ON ua.target_id = p.id
            WHERE ua.user_id = %s
              AND ua.created_at >= DATE_SUB(NOW(), INTERVAL %s DAY)
              AND ua.action_type IN ('CLICK', 'LIKE', 'BOOKMARK', 'AI_CLICK')
              AND p.id IS NOT NULL
            ORDER BY ua.created_at DESC
        """
        
        cursor.execute(query, (user_id, days))
        results = cursor.fetchall()
        cursor.close()
        
        logger.info(f"📚 사용자 {user_id}가 실제로 본 게시글: {len(results)}개")
        return results
    
    def extract_keywords_from_viewed_posts(self, viewed_posts: List[Dict]) -> Dict[str, float]:
        """본 게시글들에서 실제 키워드 추출 (TF-IDF 스타일)"""
        if not viewed_posts:
            return {}
        
        # 모든 게시글의 제목/내용/태그를 합쳐서 키워드 추출
        all_text = []
        keyword_weights = defaultdict(float)
        action_weights = {
            'CLICK': 1.0,
            'LIKE': 2.0,
            'BOOKMARK': 3.0,
            'AI_CLICK': 4.0
        }
        
        for post in viewed_posts:
            title = (post.get('title') or '').lower()
            content = (post.get('content') or '').lower()
            tags = (post.get('tags') or '').lower()
            action_type = post.get('action_type', 'CLICK')
            
            # 액션 타입별 가중치
            weight = action_weights.get(action_type, 1.0)
            
            # 제목/내용/태그에서 단어 추출 (2글자 이상)
            words = re.findall(r'\b\w{2,}\b', title + ' ' + content + ' ' + tags)
            
            for word in words:
                # 불용어 제거 (한글 1-2글자, 영어 1-2글자 등)
                if len(word) >= 2:
                    keyword_weights[word] += weight
        
        # 정규화 (빈도 기반)
        total_weight = sum(keyword_weights.values())
        if total_weight > 0:
            normalized = {k: v / total_weight for k, v in keyword_weights.items()}
            # 상위 50개 키워드만 반환
            top_keywords = dict(sorted(normalized.items(), key=lambda x: x[1], reverse=True)[:50])
            logger.info(f"🔑 추출된 키워드: {len(top_keywords)}개 (상위 10개: {list(top_keywords.keys())[:10]})")
            return top_keywords
        
        return {}
    
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
        """
        사용자 선호도 분석

        - AI 프로필(고정 선호도)은 **최근 AI 분석 완료 시점 이후에 아직 활동이 없을 때만** 사용
        - 그 이후로 클릭/좋아요/참여/댓글 등의 활동이 생기면, **활동 로그 기반 선호도만** 사용
        - 이렇게 해서 "AI 분석 다시하기(리셋) 이후에는 선택한 카테고리 위주 → 활동이 쌓이면 최근 활동 위주" 흐름을 만든다.
        """
        # 최근 활동 로그
        activities = self.get_user_activities(user_id)

        # 사용자가 직접 선택한 카테고리 선호도 (AI 프로필)
        user_prefs = self.get_user_preferences(user_id)

        # 카테고리 매핑 (실제 DB 카테고리: 개발, 자격증, 영어, 독서, 취업, 기타)
        category_mapping = {
            '프로그래밍': '개발',
            '언어': '영어',
            '코딩': '개발'
        }

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

        # "실제 콘텐츠를 본 활동" 정의 (리셋 이후 이 액션들이 등장하면 활동 기반 모드로 전환)
        real_activity_types = {'CLICK', 'LIKE', 'BOOKMARK', 'COMMENT', 'RECOMMEND'}

        # AI 프로필(선호도) 중 가장 최근 생성 시점
        last_pref_time: Optional[datetime] = None
        if user_prefs:
            pref_times = [pref.get('created_at') for pref in user_prefs if pref.get('created_at')]
            if pref_times:
                last_pref_time = max(pref_times)

        # 활동 로그를 "최근 AI 프로필 이후" 것만 사용
        filtered_activities = []
        if last_pref_time:
            for a in activities:
                created_at = a.get('created_at')
                if created_at and created_at >= last_pref_time:
                    filtered_activities.append(a)
        else:
            filtered_activities = list(activities)

        total_activity_count = len(filtered_activities)

        # 리셋 이후에 실제 활동(클릭/좋아요/북마크/댓글/참여 등)이 몇 개 있었는지 확인
        real_activity_count_after_reset = sum(
            1 for a in filtered_activities if a.get('action_type') in real_activity_types
        )
        has_real_activity_after_reset = real_activity_count_after_reset > 0

        # 규칙:
        # - AI 프로필이 있고, 그 이후 "실제 활동"이 하나도 없으면 → 고정 프로필 모드
        # - 그 외 (실제 활동이 있거나, AI 프로필이 없는 경우) → 활동 로그 기반 모드
        use_fixed_profile = bool(user_prefs) and not has_real_activity_after_reset

        logger.info(
            f"📊 선호도 분석: 활동 {total_activity_count}개 (리셋 이후, 실제 활동 {real_activity_count_after_reset}개), "
            f"AI 프로필 존재={bool(user_prefs)}, 리셋 이후 실제 활동 여부={has_real_activity_after_reset}, "
            f"고정 프로필 사용={use_fixed_profile}"
        )

        # 점수 누적용
        category_scores = defaultdict(float)
        tag_scores = defaultdict(float)
        action_counts = Counter()

        # 1) 활동 로그 기반 점수
        for activity in filtered_activities:
            action_type = activity.get('action_type')
            if not action_type:
                continue

            action_counts[action_type] += 1
            weight = action_weights.get(action_type, 1.0)

            # 카테고리 점수
            category = activity.get('category')
            if category:
                mapped_category = category_mapping.get(category, category)
                category_scores[mapped_category] += weight

            # 태그 점수
            tags = activity.get('tags')
            if tags:
                tag_list = [t.strip() for t in tags.split(',') if t.strip()]
                for tag in tag_list:
                    tag_scores[tag] += weight

            # 검색 키워드도 태그처럼 사용
            if activity.get('target_keyword') and action_type == 'SEARCH':
                keyword = activity['target_keyword'].strip()
                if keyword:
                    tag_scores[keyword] += weight * 0.8

        total_weight = sum(action_weights.get(a, 1.0) * c for a, c in action_counts.items()) or 1.0

        # 2) 고정 프로필 (AI 프로필) 점수
        if use_fixed_profile and user_prefs:
            logger.info("⚙️ 리셋 이후 실제 활동이 없어 AI 프로필(고정 선호도)을 그대로 사용합니다.")
            for pref in user_prefs:
                category_name = pref.get('category_name')
                if not category_name:
                    continue
                mapped_category = category_mapping.get(category_name, category_name)
                preference_score = pref.get('preference_score') or 1.0
                # 활동 로그보다 강하게 반영 (리셋 직후에는 AI 선택이 중심이 되도록)
                category_scores[mapped_category] += 5.0 * preference_score
                logger.debug(f"AI 프로필 카테고리: {category_name} -> {mapped_category}, 점수 += {5.0 * preference_score}")

        # 3) 정규화된 카테고리 / 태그 계산
        normalized_categories = {}
        for cat, score in category_scores.items():
            # 고정 프로필 모드에서는 AI 선택 점수를 그대로 쓰고,
            # 활동 로그 점수는 total_weight 로 나눈 비율로 보조적으로만 사용
            if use_fixed_profile and user_prefs:
                normalized_categories[cat] = score
            else:
                normalized_categories[cat] = score / total_weight

        normalized_tags = {
            tag: (score / total_weight)
            for tag, score in tag_scores.items()
        }

        # 상위 카테고리/태그만 사용
        final_categories = dict(
            sorted(normalized_categories.items(), key=lambda x: x[1], reverse=True)[:10]
        )
        final_tags = dict(
            sorted(normalized_tags.items(), key=lambda x: x[1], reverse=True)[:20]
        )

        if final_categories:
            logger.info(f"✅ 최종 선호 카테고리: {list(final_categories.keys())}")

        return {
            'categories': final_categories,
            'tags': final_tags,
            'action_counts': dict(action_counts),
            # total_activities는 "실제 활동(클릭/좋아요/북마크/댓글/참여 등)" 개수로 재정의
            'total_activities': real_activity_count_after_reset,
            'use_fixed_profile': use_fixed_profile,
            'user_selected_categories': [pref['category_name'] for pref in user_prefs] if use_fixed_profile else []
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
        
        # analyze_user_preferences 결과에서 고정 프로필 사용 여부와 실제 활동 개수 확인
        use_fixed_profile = preferences.get('use_fixed_profile', False)
        total_activities = preferences.get('total_activities', 0)

        # 고정 프로필 모드가 아니고, 실제 활동이 1개 이상 있을 때만 "활동 로그 기반" 모드를 사용
        use_activity_only = (not use_fixed_profile) and (total_activities > 0)
        
        logger.info(
            f"📊 추천 시작: 실제 활동 {total_activities}개, "
            f"고정 프로필 사용={use_fixed_profile}, 활동 로그 기반 사용={use_activity_only}"
        )
        
        # 사용자가 선호하는 카테고리 집합 (DB 카테고리 기준: 개발, 자격증, 영어, 독서, 취업, 기타)
        # 활동 로그가 충분하면 활동 로그 기반 카테고리만 사용
        if use_activity_only:
            # 활동 로그에서 실제로 활동한 카테고리만 추출
            activities = self.get_user_activities(user_id)
            activity_categories = set()
            activity_category_counts = Counter()
            
            # 활동 로그에서 카테고리 추출 및 빈도 계산
            for activity in activities:
                category = activity.get('category')
                if category:
                    activity_categories.add(category)
                    activity_category_counts[category] += 1
                else:
                    # 카테고리가 NULL이면 검색 키워드나 제목에서 추론 시도
                    keyword = (activity.get('target_keyword') or '')
                    title = (activity.get('title') or '')
                    action_type = activity.get('action_type', '')
                    
                    # 검색 키워드나 제목에서 카테고리 추론
                    inferred = self._infer_category_from_text(f"{keyword} {title}")
                    if inferred:
                        activity_category_counts[inferred] += 0.5  # 추론된 카테고리는 가중치 낮게
                        logger.info(f"🔍 카테고리 추론: {action_type} - '{(keyword or title)[:30]}' → {inferred}")
            
            # ✅ 가장 많이 활동한 "대표 카테고리"만 사용
            #    - 사용자가 영어를 가장 많이 눌렀다면 → 영어만
            #    - 동률이 있으면 그 카테고리들만
            if activity_category_counts:
                # 최댓값 구하기
                max_count = max(activity_category_counts.values())
                # 최댓값과 같은 카테고리들만 대표 카테고리로 사용
                top_categories = [cat for cat, cnt in activity_category_counts.items() if cnt == max_count]
                preferred_categories: Set[str] = set(top_categories)
                logger.info(f"✅ 대표 카테고리만 사용 (클릭 최다 {max_count}회): {preferred_categories}")
                logger.info(f"📊 전체 카테고리별 활동 횟수: {dict(activity_category_counts)}")
                
                # 고정 프로필 무시 확인
                user_prefs = self.get_user_preferences(user_id)
                if user_prefs:
                    fixed_categories = [pref['category_name'] for pref in user_prefs]
                    logger.info(f"🚫 고정 프로필 카테고리 (완전히 무시됨): {fixed_categories}")
            else:
                # 활동 로그에 카테고리가 없으면 검색 키워드/제목에서 추론
                logger.warning("⚠️ 활동 로그에 카테고리 정보가 없습니다. 키워드에서 추론 시도...")
                inferred_categories = self._infer_categories_from_activities(activities)
                if inferred_categories:
                    preferred_categories = inferred_categories
                    logger.info(f"✅ 키워드에서 추론된 카테고리: {preferred_categories}")
                else:
                    preferred_categories: Set[str] = set()
                    logger.warning("⚠️ 카테고리 추론 실패. 본문 키워드 기반 추천만 사용합니다.")
        else:
            # 활동 로그가 없으면 고정 프로필 사용
            preferred_categories: Set[str] = set(preferences.get('categories', {}).keys())
            logger.info(f"⚠️ 활동 로그 없음 ({total_activities}개). 고정 프로필 사용: {preferred_categories}")
        
        # 협업 필터링 추천 (실제 본 게시글 기반 강화)
        cf_recommendations = []
        if use_collaborative_filtering:
            try:
                user_item_matrix = self.build_user_item_matrix()
                if user_id in user_item_matrix and len(user_item_matrix) > 1:
                    cf = CollaborativeFiltering(user_item_matrix)
                    
                    # User-based CF: 비슷한 사용자들이 좋아한 게시글
                    user_based = cf.user_based_recommend(user_id, n=limit * 2)
                    
                    # Item-based CF: 본 게시글과 유사한 게시글
                    item_based = cf.item_based_recommend(user_id, n=limit * 2)
                    
                    # 실제 본 게시글 ID 목록 (필터링용)
                    viewed_post_ids = {p['id'] for p in self.get_user_viewed_posts(user_id, days=30)}
                    
                    # 두 결과 결합 (가중 평균)
                    combined_scores = defaultdict(lambda: {'score': 0.0, 'count': 0, 'viewed_similarity': 0.0})
                    
                    for post_id, score in user_based:
                        # 본 게시글과의 유사도 보너스
                        viewed_bonus = 0.0
                        if viewed_post_ids:
                            # 본 게시글과 직접적으로 관련된 게시글은 높은 점수
                            # (협업 필터링에서 이미 계산됨)
                            pass
                        combined_scores[post_id]['score'] += score * 0.6  # User-based 가중치
                        combined_scores[post_id]['count'] += 1
                    
                    for post_id, score in item_based:
                        combined_scores[post_id]['score'] += score * 0.4  # Item-based 가중치
                        combined_scores[post_id]['count'] += 1
                    
                    # 평균 점수 계산
                    cf_recommendations = [
                        (post_id, scores['score'] / scores['count'] if scores['count'] > 0 else scores['score'])
                        for post_id, scores in combined_scores.items()
                        if post_id not in viewed_post_ids  # 본 게시글은 제외
                    ]
                    cf_recommendations.sort(key=lambda x: x[1], reverse=True)
                    cf_recommendations = cf_recommendations[:limit]
                    
                    logger.info(f"협업 필터링 추천: {len(cf_recommendations)}개 게시글 (본 게시글 {len(viewed_post_ids)}개 제외)")
            except Exception as e:
                logger.warning(f"협업 필터링 실패, 콘텐츠 기반으로 폴백: {e}")
        
        # 콘텐츠 기반 필터링 (기존 방식)
        # 활동 로그가 충분하면 활동 로그 기반 카테고리만 사용
        if use_activity_only:
            if not preferred_categories:
                # 활동 로그는 있지만 카테고리가 없으면 본문 키워드 기반 추천 사용
                logger.warning("⚠️ 활동 로그는 있지만 카테고리 정보가 없습니다. 본문 키워드 기반 추천을 사용합니다.")
                # 검색 키워드나 태그에서 카테고리 추론 시도
                activities = self.get_user_activities(user_id)
                inferred_categories = self._infer_categories_from_activities(activities)
                if inferred_categories:
                    preferred_categories = inferred_categories
                    logger.info(f"✅ 활동 로그에서 카테고리 추론: {preferred_categories}")
                content_based = self._get_content_based_recommendations(user_id, preferences, limit, preferred_categories)
            else:
                # 활동 로그 기반 카테고리가 있으면 사용
                content_based = self._get_content_based_recommendations(user_id, preferences, limit, preferred_categories)
        elif not preferences['categories'] and not preferences['tags']:
            # 선호도가 없으면 최신 게시글 반환
            content_based = self.get_recent_posts(limit)
        else:
            content_based = self._get_content_based_recommendations(user_id, preferences, limit, None)
        
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
            # 활동 로그 기반 카테고리가 있으면 무조건 그 카테고리만 사용
            if preferred_categories:
                logger.info(f"🔒 필터링: 활동 로그 기반 카테고리만 사용 - {preferred_categories}")
                logger.info(f"🚫 영어/독서 등 다른 카테고리는 완전히 제외됩니다")
            recommended_posts = self._get_posts_by_ids(list(all_post_ids), limit, preferred_categories)
            
            # 최종 점수 적용 및 필터링
            final_recommended = []
            excluded_count = 0
            
            for post in recommended_posts:
                post_id = post['id']
                post_category = post.get('category')
                
                # 활동 로그 기반 카테고리가 있으면 필터링
                if preferred_categories:
                    title = post.get('title', '') or ''
                    content = post.get('content', '') or ''
                    tags = post.get('tags', '') or ''
                    title_content = (title + ' ' + content + ' ' + tags).lower()
                    
                    # 본문 내용에서 실제 카테고리 추론 (우선)
                    inferred_categories = self._infer_categories_from_text(title_content)
                    
                    # 카테고리 필드와 본문 분석 결과 모두 고려
                    is_matched = False
                    
                    # 1. 본문 내용 기반 추론 카테고리가 선호 카테고리와 일치하는지 확인 (최우선)
                    if inferred_categories & preferred_categories:
                        is_matched = True
                        logger.debug(f"✅ 게시글 {post_id}: 본문 분석 결과 {inferred_categories} 중 선호 카테고리 매칭")
                    # 2. 카테고리 필드 직접 매칭 (보조)
                    elif post_category in preferred_categories:
                        is_matched = True
                        logger.debug(f"✅ 게시글 {post_id}: 카테고리 필드 직접 매칭 ({post_category})")
                    # 3. 본문에 선호 카테고리별 키워드가 있는지 확인 (추가 확인)
                    else:
                        for category in preferred_categories:
                            if category in CATEGORY_KEYWORDS:
                                keywords = CATEGORY_KEYWORDS[category]
                                if any(keyword.lower() in full_text for keyword in keywords):
                                    is_matched = True
                                    logger.debug(f"✅ 게시글 {post_id}: 키워드 매칭으로 {category} 카테고리로 분류")
                                    break
                    
                    if not is_matched:
                        excluded_count += 1
                        logger.debug(f"🚫 게시글 {post_id} 제외: 카테고리={post_category}, 본문추론={inferred_categories}, 선호={preferred_categories}")
                        continue
                
                post['recommendation_score'] = round(final_scores.get(post_id, 0), 2)
                post['cf_score'] = round((post['recommendation_score'] / 0.6) if post_id in cf_post_ids else 0, 2)
                post['content_score'] = round((post['recommendation_score'] / 0.4) if post_id in content_post_ids else 0, 2)
                final_recommended.append(post)
            
            if excluded_count > 0:
                logger.info(f"🔒 협업 필터링 결과 필터링: {len(final_recommended)}개 포함, {excluded_count}개 제외")
            
            # 점수 기준으로 정렬한 뒤, 상위권(top_k) 안에서 랜덤하게 섞어서
            # 새로고침마다 구성이 조금씩 달라지도록 함
            final_recommended.sort(key=lambda x: x['recommendation_score'], reverse=True)
            
            top_k_size = max(limit * 2, limit)
            top_k = final_recommended[:top_k_size]
            
            import random
            random.shuffle(top_k)
            result = top_k[:limit]
            
            # 최종 필터링: 실제 본 게시글 기반으로 필터링
            if preferred_categories and result:
                # 실제 본 게시글들 조회
                viewed_posts = self.get_user_viewed_posts(user_id, days=30)
                extracted_keywords = self.extract_keywords_from_viewed_posts(viewed_posts)
                
                final_filtered = []
                excluded_categories = set()
                
                for post in result:
                    post_category = post.get('category')
                    title = post.get('title', '') or ''
                    content = post.get('content', '') or ''
                    tags = post.get('tags', '') or ''
                    full_text = (title + ' ' + content + ' ' + tags).lower()
                    
                    # 1. 실제 본 게시글과의 유사도 확인 (가장 중요)
                    similarity = 0.0
                    if viewed_posts:
                        similarity = self._calculate_post_similarity(post, viewed_posts, extracted_keywords)
                    
                    # 2. 본문 내용에서 실제 카테고리 추론 (제목+내용+태그)
                    inferred_categories = self._infer_categories_from_text(full_text)
                    
                    # 선호 카테고리에 속하는지 확인
                    is_preferred = False
                    
                    # 1. 실제 본 게시글과 유사도가 높으면 포함 (가장 우선)
                    if similarity > 5.0:  # 유사도 임계값
                        is_preferred = True
                        logger.debug(f"✅ 게시글 {post['id']}: 본 게시글과 유사도 높음 ({similarity:.2f})")
                    # 2. 본문 내용 기반 추론 카테고리가 선호 카테고리와 일치하는지 확인 (우선)
                    elif inferred_categories & preferred_categories:
                        is_preferred = True
                        logger.debug(f"✅ 게시글 {post['id']}: 본문 분석 결과 {inferred_categories} 중 선호 카테고리 매칭")
                    # 3. 카테고리 필드 직접 매칭 (보조)
                    elif post_category in preferred_categories:
                        is_preferred = True
                        logger.debug(f"✅ 게시글 {post['id']}: 카테고리 필드 직접 매칭 ({post_category})")
                    # 4. 키워드 직접 매칭 확인 (추가 확인)
                    else:
                        for category in preferred_categories:
                            if category in CATEGORY_KEYWORDS:
                                keywords = CATEGORY_KEYWORDS[category]
                                if any(keyword.lower() in full_text for keyword in keywords):
                                    is_preferred = True
                                    logger.debug(f"✅ 게시글 {post['id']}: 키워드 매칭으로 {category} 카테고리로 분류")
                                    break
                    
                    if is_preferred:
                        final_filtered.append(post)
                    else:
                        excluded_categories.add(post_category)
                        logger.warning(f"🚫 최종 필터링 제외: 게시글 {post['id']} (카테고리필드: {post_category}, 본문추론: {inferred_categories}, 제목: {title[:50]}, 유사도: {similarity:.2f})")
                
                if final_filtered:
                    logger.info(f"🔒 최종 필터링: {len(final_filtered)}개 포함, 제외된 카테고리: {excluded_categories}")
                    result_categories = Counter(p.get('category', 'NULL') for p in final_filtered)
                    logger.info(f"✅ 최종 추천 결과: {len(final_filtered)}개 게시글, 카테고리 분포: {dict(result_categories)}")
                    return final_filtered[:limit]
                else:
                    logger.error(f"❌ 최종 필터링 후 게시글이 없습니다! 선호 카테고리: {preferred_categories}")
                    return []  # 빈 결과 반환
            
            # 최종 결과 로그
            if result:
                result_categories = Counter(p.get('category', 'NULL') for p in result)
                logger.info(f"✅ 최종 추천 결과: {len(result)}개 게시글, 카테고리 분포: {dict(result_categories)}")
            
            return result
        else:
            # 협업 필터링 결과가 없으면 콘텐츠 기반만 사용
            # 활동 로그 기반 카테고리가 있으면 필터링 강화
            if preferred_categories:
                logger.info(f"🔒 콘텐츠 기반 추천 필터링: 활동 로그 기반 카테고리만 사용 - {preferred_categories}")
                logger.info(f"🚫 영어/독서 등 다른 카테고리는 완전히 제외됩니다")
                
                # 강력한 필터링: 카테고리 직접 매칭 또는 키워드 매칭
                filtered = []
                excluded_count = 0
                
                for post in content_based:
                    post_category = post.get('category')
                    title = post.get('title', '') or ''
                    content = post.get('content', '') or ''
                    tags = post.get('tags', '') or ''
                    full_text = (title + ' ' + content + ' ' + tags).lower()
                    
                    # 본문 내용에서 실제 카테고리 추론 (제목+내용+태그)
                    inferred_categories = self._infer_categories_from_text(full_text)
                    
                    # 선호 카테고리에 속하는지 확인
                    matched = False
                    
                    # 1. 본문 내용 기반 추론 카테고리가 선호 카테고리와 일치하는지 확인 (우선)
                    if inferred_categories & preferred_categories:
                        matched = True
                        logger.debug(f"✅ 게시글 {post['id']}: 본문 분석 결과 {inferred_categories} 중 선호 카테고리 매칭")
                    # 2. 카테고리 필드 직접 매칭 (보조)
                    elif post_category in preferred_categories:
                        matched = True
                        logger.debug(f"✅ 게시글 {post['id']}: 카테고리 필드 직접 매칭 ({post_category})")
                    # 3. 키워드 직접 매칭 확인 (추가 확인)
                    else:
                        for category in preferred_categories:
                            if category in CATEGORY_KEYWORDS:
                                keywords = CATEGORY_KEYWORDS[category]
                                if any(keyword.lower() in full_text for keyword in keywords):
                                    matched = True
                                    logger.debug(f"✅ 게시글 {post['id']}: 키워드 매칭으로 {category} 카테고리로 분류")
                                    break
                        
                    if matched:
                        filtered.append(post)
                    else:
                        excluded_count += 1
                        logger.debug(f"🚫 게시글 {post['id']} 제외: 카테고리필드={post_category}, 본문추론={inferred_categories}, 제목={title[:50]}")
                
                if filtered:
                    logger.info(f"✅ 필터링 후 {len(filtered)}개 게시글 포함, {excluded_count}개 제외 (원본: {len(content_based)}개)")
                    result = filtered[:limit]
                    
                    # 최종 한 번 더 필터링
                    final_result = []
                    for post in result:
                        post_category = post.get('category')
                        title = post.get('title', '') or ''
                        content = post.get('content', '') or ''
                        tags = post.get('tags', '') or ''
                        full_text = (title + ' ' + content + ' ' + tags).lower()
                        
                        # 본문 내용에서 실제 카테고리 추론 (제목+내용+태그)
                        inferred_categories = self._infer_categories_from_text(full_text)
                        
                        matched = False
                        # 1. 본문 내용 기반 추론 카테고리가 선호 카테고리와 일치하는지 확인 (우선)
                        if inferred_categories & preferred_categories:
                            matched = True
                        # 2. 카테고리 필드 직접 매칭 (보조)
                        elif post_category in preferred_categories:
                            matched = True
                        # 3. 키워드 매칭 재확인 (추가 확인)
                        else:
                            for category in preferred_categories:
                                if category in CATEGORY_KEYWORDS:
                                    keywords = CATEGORY_KEYWORDS[category]
                                    if any(keyword.lower() in full_text for keyword in keywords):
                                        matched = True
                                        break
                        
                        if matched:
                            final_result.append(post)
                        else:
                            logger.warning(
                                f"🚫 최종 필터링 제외: 게시글 {post['id']} "
                                f"(카테고리필드: {post_category}, 본문추론: {inferred_categories}, 제목: {title[:50]})"
                            )
                    
                    if final_result:
                        result_categories = Counter(p.get('category', 'NULL') for p in final_result)
                        logger.info(f"✅ 최종 추천 결과: {len(final_result)}개 게시글, 카테고리 분포: {dict(result_categories)}")
                        return final_result
                    else:
                        logger.error(f"❌ 최종 필터링 후 게시글이 없습니다!")
                        return []
                else:
                    logger.warning(f"⚠️ 필터링 후 게시글이 없습니다! 활동한 카테고리({preferred_categories})에 해당하는 게시글이 없을 수 있습니다.")
                    return []  # 빈 결과 반환 (고정 프로필로 폴백되지 않도록)
            
            # 활동 로그 기반 카테고리가 없으면 콘텐츠 기반 결과도 필터링
            if preferred_categories:
                filtered = []
                for post in content_based:
                    post_category = post.get('category')
                    if post_category in preferred_categories:
                        filtered.append(post)
                if filtered:
                    logger.info(f"✅ 콘텐츠 기반 필터링: {len(filtered)}개 포함")
                    return filtered[:limit]
                return []
            
            return content_based
    
    def _calculate_post_similarity(self, post: Dict, viewed_posts: List[Dict], 
                                   extracted_keywords: Dict[str, float]) -> float:
        """게시글과 본 게시글들의 유사도 계산"""
        if not viewed_posts:
            return 0.0
        
        post_title = (post.get('title') or '').lower()
        post_content = (post.get('content') or '').lower()
        post_tags = (post.get('tags') or '').lower()
        post_text = post_title + ' ' + post_content + ' ' + post_tags
        
        similarity_score = 0.0
        
        # 1. 본 게시글들과 직접 비교 (제목/내용 유사도)
        for viewed_post in viewed_posts:
            viewed_title = (viewed_post.get('title') or '').lower()
            viewed_content = (viewed_post.get('content') or '').lower()
            viewed_tags = (viewed_post.get('tags') or '').lower()
            viewed_text = viewed_title + ' ' + viewed_content + ' ' + viewed_tags
            
            # 공통 단어 계산
            post_words = set(re.findall(r'\b\w{2,}\b', post_text))
            viewed_words = set(re.findall(r'\b\w{2,}\b', viewed_text))
            
            if post_words and viewed_words:
                common_words = post_words & viewed_words
                if common_words:
                    # Jaccard 유사도
                    jaccard = len(common_words) / len(post_words | viewed_words)
                    similarity_score += jaccard * 10  # 가중치
        
        # 2. 추출된 키워드와 매칭
        post_words = set(re.findall(r'\b\w{2,}\b', post_text))
        for keyword, weight in extracted_keywords.items():
            if keyword in post_words:
                similarity_score += weight * 100  # 키워드 매칭은 높은 점수
        
        # 3. 카테고리 매칭
        post_category = post.get('category')
        viewed_categories = Counter(p.get('category') for p in viewed_posts if p.get('category'))
        if post_category in viewed_categories:
            similarity_score += viewed_categories[post_category] * 5
        
        # 4. 태그 매칭
        if post_tags:
            post_tag_list = [t.strip() for t in post_tags.split(',') if t.strip()]
            for viewed_post in viewed_posts:
                viewed_tags = (viewed_post.get('tags') or '').lower()
                if viewed_tags:
                    viewed_tag_list = [t.strip() for t in viewed_tags.split(',') if t.strip()]
                    common_tags = set(post_tag_list) & set(viewed_tag_list)
                    if common_tags:
                        similarity_score += len(common_tags) * 3
        
        return similarity_score
    
    def _get_content_based_recommendations(self, user_id: int, preferences: Dict, limit: int, 
                                          activity_only_categories: Optional[Set[str]] = None) -> List[Dict]:
        """콘텐츠 기반 추천 (실제 본 게시글 기반)
        
        Args:
            user_id: 사용자 ID
            preferences: 선호도 정보
            limit: 추천 개수
            activity_only_categories: 활동 로그 기반 카테고리만 사용할 경우 (None이면 모든 선호 카테고리 사용)
        """
        # 사용자가 실제로 본 게시글들 조회
        viewed_posts = self.get_user_viewed_posts(user_id, days=30)
        extracted_keywords = self.extract_keywords_from_viewed_posts(viewed_posts)
        
        cursor = self.conn.cursor(dictionary=True)
        
        # 카테고리 매핑
        category_mapping = {
            '프로그래밍': '개발',
            '언어': '영어',
            '코딩': '개발'
        }
        
        # 활동 로그 기반 카테고리만 사용할 경우
        if activity_only_categories is not None:
            if activity_only_categories:
                categories = list(activity_only_categories)
                logger.info(f"활동 로그 기반 카테고리만 사용: {categories}")
            else:
                # 활동 로그는 있지만 카테고리가 없으면 본문 키워드 기반 추천만 사용
                categories = []
                logger.info("활동 로그는 있지만 카테고리 정보가 없습니다. 본문 키워드 기반 추천만 사용합니다.")
        else:
            raw_categories = list(preferences['categories'].keys())[:5]
            categories = [category_mapping.get(cat, cat) for cat in raw_categories]
            categories = [cat for cat in dict.fromkeys(categories) if cat]
        
        # 실제 본 게시글에서 태그 추출 (하드코딩된 태그보다 우선)
        tags = []
        if viewed_posts:
            # 본 게시글들의 태그 수집
            viewed_tags = Counter()
            for post in viewed_posts:
                post_tags = post.get('tags', '')
                if post_tags:
                    tag_list = [t.strip() for t in post_tags.split(',') if t.strip()]
                    for tag in tag_list:
                        viewed_tags[tag] += 1
            # 상위 10개 태그 사용
            tags = [tag for tag, _ in viewed_tags.most_common(10)]
            logger.info(f"📌 본 게시글에서 추출한 태그: {tags[:5]}")
        
        # 하드코딩된 태그는 보조로만 사용
        if not tags:
            tags = list(preferences['tags'].keys())[:10]
        
        # 선호 카테고리/태그가 전혀 없으면 최신 글로 폴백
        # 단, 활동 로그가 충분하면 본문 키워드 기반 추천 사용
        if not categories and not tags:
            if activity_only_categories is not None:
                # 활동 로그가 충분하면 본문 키워드 기반 추천 사용
                categories = []  # 빈 카테고리로 두고, WHERE 절에서 개발 키워드 조건만 사용
            else:
                return self.get_recent_posts(limit)
        
        # 태그 조건
        tag_conditions = " OR ".join([f"p.tags LIKE %s" for _ in tags]) if tags else "FALSE"
        
        # 사용자가 활동한 카테고리별 키워드 추출
        # 각 카테고리에 대해 본문 키워드 조건을 동적으로 생성
        category_keyword_conditions = []
        category_keyword_params = []
        
        for category in categories:
            if category in CATEGORY_KEYWORDS:
                keywords = CATEGORY_KEYWORDS[category]
                # 각 키워드에 대해 제목/본문 조건 생성
                keyword_conditions = " OR ".join([f"(p.title LIKE %s OR p.content LIKE %s)" for _ in keywords])
                category_keyword_conditions.append(f"({keyword_conditions})")
                # 파라미터 추가 (각 키워드마다 2개씩: 제목, 본문)
                for keyword in keywords:
                    category_keyword_params.extend([f"%{keyword}%", f"%{keyword}%"])
        
        # 모든 카테고리의 키워드 조건을 OR로 결합
        if category_keyword_conditions:
            all_keyword_conditions = " OR ".join(category_keyword_conditions)
        else:
            all_keyword_conditions = "FALSE"
        
        # 사용자가 실제로 선호/활동한 카테고리에 속한 글만 추천하도록 강하게 제한
        # - p.category는 반드시 선호 카테고리 중 하나여야 함
        # - 또는 본문에 개발 관련 키워드가 있으면 개발 카테고리로 간주
        # - 태그는 점수 계산 및 추가 필터링에만 사용
        # - 카테고리가 비어있으면 개발 키워드 조건만 사용
        
        # WHERE 절 조건 구성
        # 활동 로그 기반 카테고리가 있으면 무조건 그 카테고리만 사용 (강제 필터링)
        if categories:
            category_condition = f"p.category IN ({','.join(['%s'] * len(categories))})"
            category_match_case = f"WHEN p.category IN ({','.join(['%s'] * len(categories))}) THEN 1 ELSE 0"
            # 활동 로그 기반 카테고리가 있으면 키워드 조건도 추가 (본문에 키워드가 있어도 해당 카테고리로 분류)
            where_condition = f"({category_condition} OR ({all_keyword_conditions}))"
            logger.info(f"🔒 SQL 필터링: 카테고리={categories}, 키워드 조건 포함")
        else:
            # 카테고리가 비어있으면 키워드 조건만 사용
            category_condition = "FALSE"
            category_match_case = "0"
            where_condition = f"({all_keyword_conditions})"
            logger.info(f"🔒 SQL 필터링: 카테고리 없음, 키워드 조건만 사용")
        
        query = f"""
            SELECT 
                p.id,
                p.title,
                p.category,
                p.tags,
                p.content,
                p.view_count,
                p.like_count,
                p.created_at,
                CASE 
                    {category_match_case}
                END as category_match,
                CASE 
                    WHEN ({tag_conditions}) THEN 1 ELSE 0 
                END as tag_match,
                CASE 
                    WHEN ({all_keyword_conditions}) THEN 1 ELSE 0 
                END as keyword_match
            FROM posts p
            WHERE p.id NOT IN (
                SELECT DISTINCT target_id 
                FROM user_activity 
                WHERE user_id = %s AND target_id IS NOT NULL
            )
            AND {where_condition}
            ORDER BY 
                category_match DESC,
                keyword_match DESC,
                tag_match DESC,
                (p.like_count * 2 + p.view_count) DESC,
                p.created_at DESC
            LIMIT %s
        """
        
        # 파라미터 순서:
        # 1) category_match IN (...) 용 카테고리 목록 (카테고리가 있을 때만)
        # 2) tag_match 조건 / 태그 필터용 LIKE 파라미터
        # 3) keyword_match 조건 (제목/본문에 카테고리별 키워드가 있는지)
        # 4) 이미 본 글 제외를 위한 user_id
        # 5) WHERE p.category IN (...) 필터용 카테고리 목록 (카테고리가 있을 때만)
        # 6) keyword_match 조건 (WHERE 절에서 사용)
        params = []
        # 1) category_match IN (...)
        if categories:
            params.extend(categories)
        # 2) tag_match용 태그
        if tags:
            params.extend([f"%{tag}%" for tag in tags])
        # 3) keyword_match용 카테고리별 키워드 (제목/본문)
        params.extend(category_keyword_params)
        # 4) user_id
        params.append(user_id)
        # 5) WHERE p.category IN (...)
        if categories:
            params.extend(categories)
        # 6) keyword_match 조건 (WHERE 절에서 사용)
        params.extend(category_keyword_params)
        params.append(limit)
        
        cursor.execute(query, params)
        results = cursor.fetchall()
        cursor.close()
        
        # 점수 계산 (실제 본 게시글 기반)
        scored_posts = []
        for post in results:
            score = 0
            
            post_category = post['category']
            mapped_category = category_mapping.get(post_category, post_category)
            
            # 1. 실제 본 게시글들과의 유사도 계산 (가장 중요 - 80% 가중치)
            if viewed_posts:
                similarity = self._calculate_post_similarity(post, viewed_posts, extracted_keywords)
                score += similarity * 0.8  # 실제 본 게시글 기반 유사도가 가장 중요
                logger.debug(f"게시글 {post['id']}: 본 게시글과 유사도={similarity:.2f}")
            else:
                # 본 게시글이 없으면 하드코딩된 키워드 사용
                logger.debug(f"게시글 {post['id']}: 본 게시글이 없어서 키워드 기반 점수만 사용")
            
            # 2. 본문 내용 분석: 각 카테고리별 키워드가 있는지 확인 (보조 - 20% 가중치)
            title_content = ((post.get('title', '') or '') + ' ' + (post.get('content', '') or '')).lower()
            matched_categories = []
            
            # 사용자가 활동한 각 카테고리에 대해 키워드 매칭 확인
            keyword_score = 0
            for category in categories:
                if category in CATEGORY_KEYWORDS:
                    keywords = CATEGORY_KEYWORDS[category]
                    has_keywords = any(keyword.lower() in title_content for keyword in keywords)
                    if has_keywords:
                        matched_categories.append(category)
                        keyword_score += 50  # 하드코딩된 키워드는 낮은 점수 (보조)
                        logger.debug(f"게시글 {post['id']}: 본문에 {category} 카테고리 키워드 발견")
            
            score += keyword_score * 0.2  # 키워드 점수는 20%만 반영
            
            # 활동 로그 기반 카테고리만 사용할 경우, 키워드 매칭이 있으면 해당 카테고리로 강제 분류
            if activity_only_categories and matched_categories:
                for matched_cat in matched_categories:
                    if matched_cat in activity_only_categories:
                        score += 150  # 활동한 카테고리의 키워드가 있으면 추가 점수
                        logger.debug(f"게시글 {post['id']}: 활동한 카테고리({matched_cat}) 키워드 매칭 보너스")
            
            # 활동 로그 기반 카테고리만 사용할 경우
            if activity_only_categories:
                # 본문 내용에서 실제 카테고리 추론 (제목+내용+태그)
                title = post.get('title', '') or ''
                content = post.get('content', '') or ''
                tags = post.get('tags', '') or ''
                full_text = (title + ' ' + content + ' ' + tags).lower()
                inferred_from_content = self._infer_categories_from_text(full_text)
                
                # 본문 기반 추론 카테고리가 활동한 카테고리와 일치하는지 확인 (최우선)
                if inferred_from_content & activity_only_categories:
                    score += 600  # 본문 분석 결과가 활동한 카테고리와 일치하면 매우 높은 점수
                    logger.debug(f"✅ 게시글 {post['id']}: 본문 분석 결과 {inferred_from_content} 중 활동한 카테고리 매칭 +600점")
                # 카테고리 필드 직접 매칭
                elif post_category in activity_only_categories:
                    score += 500  # 활동한 카테고리 직접 매칭은 높은 점수
                    logger.debug(f"✅ 게시글 {post['id']}: 카테고리 필드({post_category}) 직접 매칭 +500점")
                # 키워드 매칭
                elif matched_categories and any(cat in activity_only_categories for cat in matched_categories):
                    score += 400  # 키워드 매칭도 높은 점수
                    logger.debug(f"✅ 게시글 {post['id']}: 활동한 카테고리 키워드 매칭 +400점")
                else:
                    # 활동한 카테고리가 아니면 점수를 크게 낮춤
                    score -= 1000  # 활동하지 않은 카테고리는 매우 낮은 점수
                    logger.debug(f"🚫 게시글 {post['id']}: 활동하지 않은 카테고리(필드={post_category}, 본문추론={inferred_from_content}) -1000점")
            else:
                # 고정 프로필 사용 시
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
            post['matched_categories'] = matched_categories  # 디버깅용
            scored_posts.append(post)
        
        # 점수 기준으로 정렬한 뒤, 상위권(top_k) 안에서 랜덤하게 섞어서
        # 새로고침마다 구성이 조금씩 달라지도록 함
        scored_posts.sort(key=lambda x: x['recommendation_score'], reverse=True)
        
        # 활동 로그 기반 카테고리가 있으면 최종 필터링 (점수가 양수인 게시글만)
        if activity_only_categories:
            # 점수가 양수인 게시글만 사용 (활동한 카테고리 또는 키워드 매칭된 게시글)
            filtered_scored = [p for p in scored_posts if p['recommendation_score'] > 0]
            if filtered_scored:
                logger.info(f"✅ 최종 필터링: {len(filtered_scored)}개 게시글 (원본: {len(scored_posts)}개)")
                scored_posts = filtered_scored
            else:
                logger.warning(f"⚠️ 필터링 후 게시글이 없습니다. 원본 사용")
        
        top_k_size = max(limit * 2, limit)
        top_k = scored_posts[:top_k_size]
        
        import random
        random.shuffle(top_k)
        result = top_k[:limit]
        
        # 활동 로그 기반 카테고리가 있으면 최종 필터링 (영어/독서 강제 제외)
        if activity_only_categories and result:
            final_filtered = []
            excluded = []
            
            for post in result:
                post_category = post.get('category')
                title = post.get('title', '') or ''
                content = post.get('content', '') or ''
                tags = post.get('tags', '') or ''
                full_text = (title + ' ' + content + ' ' + tags).lower()
                
                # 본문 내용에서 실제 카테고리 추론 (제목+내용+태그)
                inferred_categories = self._infer_categories_from_text(full_text)
                
                # 선호 카테고리에 속하는지 확인
                is_allowed = False
                
                # 1. 본문 내용 기반 추론 카테고리가 활동한 카테고리와 일치하는지 확인 (우선)
                if inferred_categories & activity_only_categories:
                    is_allowed = True
                    logger.debug(f"✅ 게시글 {post['id']}: 본문 분석 결과 {inferred_categories} 중 활동한 카테고리 매칭")
                # 2. 카테고리 필드 직접 매칭 (보조)
                elif post_category in activity_only_categories:
                    is_allowed = True
                    logger.debug(f"✅ 게시글 {post['id']}: 카테고리 필드 직접 매칭 ({post_category})")
                # 3. 키워드 매칭 확인 (추가 확인)
                else:
                    for category in activity_only_categories:
                        if category in CATEGORY_KEYWORDS:
                            keywords = CATEGORY_KEYWORDS[category]
                            if any(keyword.lower() in full_text for keyword in keywords):
                                is_allowed = True
                                logger.debug(f"✅ 게시글 {post['id']}: 키워드 매칭으로 {category} 카테고리로 분류")
                                break
                
                if is_allowed:
                    final_filtered.append(post)
                else:
                    excluded.append((post['id'], post_category))
                    logger.warning(f"🚫 최종 필터링 제외: 게시글 {post['id']} (카테고리필드: {post_category}, 본문추론: {inferred_categories}, 제목: {title[:50]})")
            
            if final_filtered:
                logger.info(f"🔒 최종 필터링: {len(final_filtered)}개 포함, {len(excluded)}개 제외")
                result_categories = Counter(p.get('category', 'NULL') for p in final_filtered)
                logger.info(f"✅ 최종 추천 결과: {len(final_filtered)}개 게시글, 카테고리 분포: {dict(result_categories)}")
                return final_filtered
            else:
                logger.error(f"❌ 최종 필터링 후 게시글이 없습니다! 선호 카테고리: {activity_only_categories}")
                return []
        
        # 최종 결과 로그
        if result:
            result_categories = Counter(p.get('category', 'NULL') for p in result)
            logger.info(f"✅ 최종 추천 결과: {len(result)}개 게시글, 카테고리 분포: {dict(result_categories)}")
        
        return result
    
    def _get_posts_by_ids(self, post_ids: List[int], limit: int, preferred_categories: Optional[Set[str]] = None) -> List[Dict]:
        """게시글 ID 리스트로 게시글 조회
        
        preferred_categories가 주어지면, 해당 카테고리에 속한 게시글만 반환합니다.
        본문 내용을 분석하여 각 카테고리별 키워드가 있으면 해당 카테고리로 간주합니다.
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
                p.content,
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
        
        # 선호 카테고리가 있다면 그 안에 속한 게시글만 남김 (강제 필터링)
        # 본문 내용을 분석하여 각 카테고리별 키워드가 있으면 해당 카테고리로 간주
        if preferred_categories:
            filtered = []
            excluded_count = 0
            for r in results:
                post_category = r.get('category')
                title = r.get('title', '') or ''
                content = r.get('content', '') or ''
                tags = r.get('tags', '') or ''
                full_text = (title + ' ' + content + ' ' + tags).lower()
                
                # 본문 내용에서 실제 카테고리 추론 (제목+내용+태그)
                inferred_categories = self._infer_categories_from_text(full_text)
                
                matched = False
                
                # 1. 본문 내용 기반 추론 카테고리가 선호 카테고리와 일치하는지 확인 (우선)
                if inferred_categories & preferred_categories:
                    matched = True
                    logger.info(f"✅ 게시글 {r['id']}: 본문 분석 결과 {inferred_categories} 중 선호 카테고리 매칭")
                # 2. 카테고리 필드 직접 매칭 (보조)
                elif post_category in preferred_categories:
                    matched = True
                    logger.debug(f"✅ 게시글 {r['id']}: 카테고리 필드 직접 매칭 ({post_category})")
                # 3. 본문에 선호 카테고리별 키워드가 있는지 확인 (추가 확인)
                else:
                    for category in preferred_categories:
                        if category in CATEGORY_KEYWORDS:
                            keywords = CATEGORY_KEYWORDS[category]
                            has_keywords = any(keyword.lower() in full_text for keyword in keywords)
                            if has_keywords:
                                logger.info(f"✅ 게시글 {r['id']}: 본문에 {category} 카테고리 키워드 발견, {category} 카테고리로 분류")
                                matched = True
                                break
                    
                if matched:
                    filtered.append(r)
                else:
                    excluded_count += 1
                    logger.debug(
                        f"🚫 게시글 {r['id']} 제외: 카테고리필드={post_category}, "
                        f"본문추론={inferred_categories}, 제목={title[:50]}, 선호 카테고리={preferred_categories}"
                    )
            
            logger.info(f"🔒 필터링 결과: {len(filtered)}개 포함, {excluded_count}개 제외 (총 {len(results)}개)")
            
            # 필터 결과가 비면 경고 (활동한 카테고리 게시글이 없는 경우)
            if not filtered:
                logger.warning(f"⚠️ 필터링 후 게시글이 없습니다! 활동한 카테고리({preferred_categories})에 해당하는 게시글이 없을 수 있습니다.")
                # 빈 결과 반환 (고정 프로필로 폴백되지 않도록)
                return []
            
            return filtered
        
        return results
    
    def _infer_category_from_text(self, text: str) -> Optional[str]:
        """텍스트에서 카테고리 추론 (단일 카테고리 반환)"""
        if not text:
            return None
        
        text_lower = text.lower()
        
        # 우선순위: 개발 > 취업 > 자격증 > 기타 > 독서 > 영어
        priority_order = ['개발', '취업', '자격증', '기타', '독서', '영어']
        
        # 각 카테고리별 키워드 확인 (우선순위 순)
        for category in priority_order:
            if category in CATEGORY_KEYWORDS:
                keywords = CATEGORY_KEYWORDS[category]
            if any(keyword.lower() in text_lower for keyword in keywords):
                return category
        
        return None
    
    def _infer_categories_from_text(self, text: str) -> Set[str]:
        """텍스트에서 여러 카테고리 추론 (예: "개발자 취업을 위한 독서토론" → {'개발', '취업'})
        
        본문 내용을 분석하여 실제 주제를 파악합니다.
        카테고리 필드보다 본문 내용이 더 정확한 경우가 많습니다.
        """
        if not text:
            return set()
        
        text_lower = text.lower()
        inferred = set()
        category_scores = defaultdict(int)
        
        # 각 카테고리별로 키워드 매칭 확인 및 점수 계산
        for category, keywords in CATEGORY_KEYWORDS.items():
            score = 0
            for keyword in keywords:
                # 키워드가 텍스트에 포함되어 있으면 점수 증가
                if keyword.lower() in text_lower:
                    # 중요한 키워드(긴 단어, 전문 용어)는 높은 점수
                    if len(keyword) >= 4:
                        score += 3
                    else:
                        score += 1
            
            if score > 0:
                category_scores[category] = score
                inferred.add(category)
        
        # 점수가 높은 카테고리만 반환 (최대 3개, 점수 2 이상)
        if category_scores:
            sorted_categories = sorted(category_scores.items(), key=lambda x: x[1], reverse=True)
            top_categories = [cat for cat, score in sorted_categories if score >= 2][:3]
            if top_categories:
                logger.debug(f"🔍 텍스트에서 추론된 카테고리: {top_categories} (점수: {dict(category_scores)})")
                return set(top_categories)
        
        return inferred
    
    def _infer_categories_from_activities(self, activities: List[Dict]) -> Set[str]:
        """활동 로그에서 카테고리를 추론 (검색 키워드, 태그, 제목 분석)"""
        inferred = set()
        category_counts = Counter()
        
        for activity in activities:
            # 검색 키워드 / 제목 / 상세 내용 (None 방지)
            keyword = (activity.get('target_keyword') or '')
            title = (activity.get('title') or '')
            action_detail = (activity.get('action_detail') or '')
            
            # 모든 텍스트를 합쳐서 분석
            full_text = ' '.join([keyword, title, action_detail]).strip()
            inferred_category = self._infer_category_from_text(full_text)
            
            if inferred_category:
                inferred.add(inferred_category)
                category_counts[inferred_category] += 1
        
        # 가장 많이 추론된 카테고리만 반환 (최대 2개)
        if category_counts:
            top_categories = [cat for cat, _ in category_counts.most_common(2)]
            logger.info(f"🔍 키워드에서 추론된 카테고리: {top_categories} (빈도: {dict(category_counts)})")
            return set(top_categories)
        
        return inferred
    
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
        
        # 최종 카테고리 분포 로그
        if recommended_posts:
            final_categories = Counter(p.get('category', 'NULL') for p in recommended_posts)
            logger.info(f"✅ 최종 추천 완료: {len(recommended_posts)}개 게시글, 카테고리 분포: {dict(final_categories)}")
        else:
            logger.warning(f"⚠️ 추천 결과가 비어있습니다!")
        
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

