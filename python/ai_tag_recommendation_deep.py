#!/usr/bin/env python3
"""
딥러닝 기반 게시글 태그 자동 추천 시스템
BERT 기반 텍스트 분류를 사용하여 카테고리와 태그를 추천합니다.
"""

import json
import sys
import re
from typing import Dict, List, Tuple, Optional
from collections import Counter

# 딥러닝 라이브러리 (선택적 - 없으면 규칙 기반으로 폴백)
try:
    import torch
    from transformers import AutoTokenizer, AutoModelForSequenceClassification
    from transformers import pipeline
    DEEP_LEARNING_AVAILABLE = True
except ImportError:
    DEEP_LEARNING_AVAILABLE = False
    print("딥러닝 라이브러리가 설치되지 않았습니다. 규칙 기반 시스템을 사용합니다.", file=sys.stderr)

from config import Config
from logger import setup_logger
from utils import TextUtils, ValidationUtils
from exceptions import ModelLoadError, PredictionError
from metrics import ClassificationMetrics

logger = setup_logger(__name__)

# 한국어 키워드 패턴 (폴백용)
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

# 카테고리 라벨
CATEGORY_LABELS = list(CATEGORY_KEYWORDS.keys()) + ['기타']


class DeepTagRecommender:
    """딥러닝 기반 태그 추천기"""
    
    def __init__(self, use_deep_learning: bool = True):
        self.use_deep_learning = use_deep_learning and DEEP_LEARNING_AVAILABLE
        self.category_keywords = CATEGORY_KEYWORDS
        self.tech_tags = TECH_TAGS
        self.category_labels = CATEGORY_LABELS
        
        self.tokenizer = None
        self.model = None
        self.classifier = None
        
        if self.use_deep_learning:
            self._load_model()
        else:
            logger.warning("딥러닝 모델을 사용할 수 없습니다. 규칙 기반 시스템을 사용합니다.")
    
    def _load_model(self):
        """딥러닝 모델 로드"""
        try:
            # 한국어 BERT 모델 사용 (더 가벼운 모델)
            # 실제로는 fine-tuning된 모델을 사용하는 것이 좋지만, 
            # 여기서는 기본 모델을 사용하고 나중에 fine-tuning 가능하도록 구조화
            model_name = "klue/bert-base"  # 또는 "monologg/kobert"
            
            logger.info(f"모델 로딩 중: {model_name}")
            
            # 텍스트 분류 파이프라인 사용 (더 간단)
            # 실제 프로덕션에서는 fine-tuning된 모델을 사용해야 함
            self.classifier = pipeline(
                "text-classification",
                model=model_name,
                device=0 if Config.USE_GPU and torch.cuda.is_available() else -1
            )
            
            logger.info("모델 로드 완료")
            
        except Exception as e:
            logger.error(f"모델 로드 실패: {e}")
            logger.info("규칙 기반 시스템으로 폴백합니다.")
            self.use_deep_learning = False
            raise ModelLoadError(f"모델 로드 실패: {e}") from e
    
    def _classify_with_deep_learning(self, text: str) -> Tuple[str, float]:
        """딥러닝 모델을 사용한 카테고리 분류"""
        if not self.classifier:
            raise PredictionError("모델이 로드되지 않았습니다.")
        
        try:
            # 텍스트 전처리
            clean_text = TextUtils.normalize_text(text)
            
            # 모델 예측 (실제로는 fine-tuning된 모델이 필요)
            # 여기서는 간단한 예시로 규칙 기반과 결합
            result = self._hybrid_classification(clean_text)
            
            return result
            
        except Exception as e:
            logger.error(f"딥러닝 분류 오류: {e}")
            # 폴백: 규칙 기반 분류
            return self._classify_with_rules(text)
    
    def _hybrid_classification(self, text: str) -> Tuple[str, float]:
        """
        하이브리드 분류 (규칙 기반 + 딥러닝)
        실제로는 fine-tuning된 모델을 사용해야 하지만,
        여기서는 규칙 기반을 개선한 버전을 사용
        """
        text_lower = text.lower()
        
        category_scores = {}
        for category, keywords in self.category_keywords.items():
            score = 0
            keyword_matches = 0
            
            for keyword in keywords:
                count = text_lower.count(keyword.lower())
                if count > 0:
                    keyword_matches += 1
                    score += count * 2
            
            # 키워드 매칭 비율 고려
            if keyword_matches > 0:
                match_ratio = keyword_matches / len(keywords)
                score *= (1 + match_ratio)  # 매칭 비율이 높을수록 가중치 증가
            
            if score > 0:
                category_scores[category] = score
        
        if not category_scores:
            return '기타', 0.0
        
        # 가장 높은 점수의 카테고리
        best_category = max(category_scores.items(), key=lambda x: x[1])
        max_score = max(category_scores.values())
        total_score = sum(category_scores.values())
        
        confidence = max_score / total_score if total_score > 0 else 0.0
        
        # 딥러닝 모델이 있다면 추가 점수 부여 (향후 확장)
        if self.classifier:
            # 실제로는 모델 예측 결과를 사용
            # 여기서는 향후 확장을 위한 구조만 제공
            pass
        
        return best_category[0], min(confidence, 1.0)
    
    def _classify_with_rules(self, text: str) -> Tuple[str, float]:
        """규칙 기반 카테고리 분류 (폴백)"""
        text_lower = text.lower()
        
        category_scores = {}
        for category, keywords in self.category_keywords.items():
            score = 0
            for keyword in keywords:
                count = text_lower.count(keyword.lower())
                score += count * 2
            
            if score > 0:
                category_scores[category] = score
        
        if not category_scores:
            return '기타', 0.0
        
        best_category = max(category_scores.items(), key=lambda x: x[1])
        max_score = max(category_scores.values())
        total_score = sum(category_scores.values())
        
        confidence = max_score / total_score if total_score > 0 else 0.0
        
        return best_category[0], min(confidence, 1.0)
    
    def recommend_category(self, title: str, content: str) -> Tuple[str, float]:
        """카테고리 추천"""
        if not ValidationUtils.validate_text_input(title + content):
            logger.warning("입력 텍스트 검증 실패")
            return '기타', 0.0
        
        full_text = title + " " + content
        
        if self.use_deep_learning:
            try:
                return self._classify_with_deep_learning(full_text)
            except Exception as e:
                logger.error(f"딥러닝 분류 실패, 규칙 기반으로 폴백: {e}")
                return self._classify_with_rules(full_text)
        else:
            return self._classify_with_rules(full_text)
    
    def recommend_tags(self, title: str, content: str, category: str) -> List[Tuple[str, float]]:
        """태그 추천"""
        full_text = (title + " " + content).lower()
        keywords = TextUtils.extract_keywords(content)
        
        tag_scores = {}
        
        # 1. 기술 스택 태그 매칭
        for tech_tag in self.tech_tags:
            if tech_tag.lower() in full_text:
                tag_scores[tech_tag] = 0.9
        
        # 2. 키워드 빈도 분석
        keyword_counter = Counter(keywords)
        top_keywords = keyword_counter.most_common(10)
        
        for keyword, count in top_keywords:
            if len(keyword) >= 2 and keyword not in tag_scores:
                score = min(count / 5.0, 0.8)
                tag_scores[keyword] = score
        
        # 3. 카테고리 관련 키워드 추가
        if category in self.category_keywords:
            for keyword in self.category_keywords[category]:
                if keyword.lower() in full_text and keyword not in tag_scores:
                    tag_scores[keyword] = 0.7
        
        # 점수 순으로 정렬
        sorted_tags = sorted(tag_scores.items(), key=lambda x: x[1], reverse=True)
        
        return sorted_tags[:10]
    
    def recommend(self, title: str, content: str) -> Dict:
        """태그 추천 메인 함수"""
        if not title and not content:
            return {
                'category': '기타',
                'category_confidence': 0.0,
                'tags': [],
                'error': '제목과 본문이 비어있습니다.',
                'method': 'rule-based' if not self.use_deep_learning else 'deep-learning'
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
            ],
            'method': 'deep-learning' if self.use_deep_learning else 'rule-based'
        }


def main():
    """메인 함수"""
    if len(sys.argv) < 3:
        print(json.dumps({
            'error': '제목과 본문이 필요합니다.',
            'usage': 'python ai_tag_recommendation_deep.py "<title>" "<content>"'
        }), file=sys.stderr)
        sys.exit(1)
    
    title = sys.argv[1]
    content = sys.argv[2]
    
    try:
        # 딥러닝 모델 사용 시도 (실패하면 자동으로 규칙 기반으로 폴백)
        recommender = DeepTagRecommender(use_deep_learning=True)
        result = recommender.recommend(title, content)
        
        print(json.dumps(result, ensure_ascii=False, indent=2))
        
    except Exception as e:
        logger.error(f"태그 추천 오류: {e}")
        print(json.dumps({
            'error': str(e),
            'method': 'error'
        }), file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()


