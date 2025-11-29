#!/usr/bin/env python3
"""
AI 기반 게시글 태그 자동 추천 시스템
게시글 제목과 본문을 분석하여 관련 태그와 카테고리를 추천합니다.
"""

import json
import sys
import re
from collections import Counter
from typing import Dict, List, Tuple

# 한국어 키워드 패턴 (카테고리별)
CATEGORY_KEYWORDS = {
    '프로그래밍': ['코딩', '프로그래밍', '개발', '소프트웨어', '알고리즘', '자바', '파이썬', '자바스크립트', 
                '스프링', '리액트', '앱', '웹', '백엔드', '프론트엔드', '데이터베이스', 'API', '프레임워크'],
    '스터디': ['스터디', '공부', '학습', '독서', '토론', '발표', '과제', '시험', '자격증', '면접', '취업'],
    '모임': ['모임', '만남', '친목', '네트워킹', '소통', '커뮤니티', '동아리', '클럽'],
    '언어': ['영어', '일본어', '중국어', '토익', '토플', '회화', '번역', '문법'],
    '취미': ['취미', '봉사', '운동', '요리', '음악', '미술', '사진', '여행', '독서'],
    '자격증': ['자격증', '시험', '합격', '공인', '인증', '자격', '면접'],
    '취업': ['취업', '면접', '포트폴리오', '이력서', '자소서', '인턴', '신입', '경력']
}

# 기술 스택 태그
TECH_TAGS = [
    'Java', 'Python', 'JavaScript', 'TypeScript', 'React', 'Vue', 'Angular',
    'Spring', 'Django', 'Flask', 'Node.js', 'Express', 'MySQL', 'PostgreSQL',
    'MongoDB', 'Redis', 'Docker', 'Kubernetes', 'AWS', 'Git', 'GitHub',
    'HTML', 'CSS', 'SCSS', 'Bootstrap', 'Tailwind', 'jQuery', 'REST API',
    'GraphQL', 'JPA', 'Hibernate', 'MyBatis', 'Spring Boot', 'Spring Security'
]


class AITagRecommender:
    """AI 태그 추천기"""
    
    def __init__(self):
        self.category_keywords = CATEGORY_KEYWORDS
        self.tech_tags = TECH_TAGS
    
    def extract_keywords(self, text: str) -> List[str]:
        """텍스트에서 키워드 추출"""
        if not text:
            return []
        
        # HTML 태그 제거
        text = re.sub(r'<[^>]+>', '', text)
        
        # 특수문자 제거 (한글, 영문, 숫자만)
        text = re.sub(r'[^\w\s가-힣]', ' ', text)
        
        # 공백으로 분리
        words = text.split()
        
        # 2글자 이상인 단어만 필터링
        keywords = [w.strip() for w in words if len(w.strip()) >= 2]
        
        return keywords
    
    def recommend_category(self, title: str, content: str) -> Tuple[str, float]:
        """카테고리 추천"""
        full_text = (title + " " + content).lower()
        
        category_scores = {}
        for category, keywords in self.category_keywords.items():
            score = 0
            for keyword in keywords:
                count = full_text.count(keyword.lower())
                score += count * 2  # 키워드 매칭 시 점수
            
            if score > 0:
                category_scores[category] = score
        
        if not category_scores:
            return '기타', 0.0
        
        # 가장 높은 점수의 카테고리
        best_category = max(category_scores.items(), key=lambda x: x[1])
        max_score = max(category_scores.values())
        total_score = sum(category_scores.values())
        
        confidence = max_score / total_score if total_score > 0 else 0.0
        
        return best_category[0], min(confidence, 1.0)
    
    def recommend_tags(self, title: str, content: str, category: str) -> List[Tuple[str, float]]:
        """태그 추천"""
        full_text = (title + " " + content).lower()
        keywords = self.extract_keywords(content)
        
        tag_scores = {}
        
        # 1. 기술 스택 태그 매칭
        for tech_tag in self.tech_tags:
            if tech_tag.lower() in full_text:
                tag_scores[tech_tag] = 0.9  # 기술 스택은 높은 신뢰도
        
        # 2. 키워드 빈도 분석
        keyword_counter = Counter(keywords)
        top_keywords = keyword_counter.most_common(10)
        
        for keyword, count in top_keywords:
            if len(keyword) >= 2 and keyword not in tag_scores:
                # 키워드가 자주 나오면 태그로 추천
                score = min(count / 5.0, 0.8)  # 최대 0.8
                tag_scores[keyword] = score
        
        # 3. 카테고리 관련 키워드 추가
        if category in self.category_keywords:
            for keyword in self.category_keywords[category]:
                if keyword.lower() in full_text and keyword not in tag_scores:
                    tag_scores[keyword] = 0.7
        
        # 점수 순으로 정렬하여 상위 태그 반환
        sorted_tags = sorted(tag_scores.items(), key=lambda x: x[1], reverse=True)
        
        # 상위 10개만 반환
        return sorted_tags[:10]
    
    def recommend(self, title: str, content: str) -> Dict:
        """태그 추천 메인 함수"""
        if not title and not content:
            return {
                'category': '기타',
                'category_confidence': 0.0,
                'tags': [],
                'error': '제목과 본문이 비어있습니다.'
            }
        
        # 카테고리 추천
        category, category_confidence = self.recommend_category(title, content)
        
        # 태그 추천
        recommended_tags = self.recommend_tags(title, content, category)
        
        # 태그 리스트 생성 (점수 0.5 이상만)
        tags = [tag for tag, score in recommended_tags if score >= 0.5]
        
        return {
            'category': category,
            'category_confidence': round(category_confidence, 2),
            'tags': tags,
            'tag_details': [
                {'tag': tag, 'confidence': round(score, 2)}
                for tag, score in recommended_tags
            ]
        }


def main():
    """메인 함수"""
    if len(sys.argv) < 3:
        print(json.dumps({
            'error': '제목과 본문이 필요합니다.',
            'usage': 'python ai_tag_recommendation.py "<title>" "<content>"'
        }), file=sys.stderr)
        sys.exit(1)
    
    title = sys.argv[1]
    content = sys.argv[2]
    
    try:
        recommender = AITagRecommender()
        result = recommender.recommend(title, content)
        
        print(json.dumps(result, ensure_ascii=False, indent=2))
        
    except Exception as e:
        print(json.dumps({
            'error': str(e)
        }), file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()

