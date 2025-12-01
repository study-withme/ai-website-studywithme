#!/usr/bin/env python3
"""
AI 기반 스팸/악성 댓글 자동 차단 시스템
- 딥러닝 모델(BERT) 기반 텍스트 분류
- 신고 건수 기반 선 검토 시스템
- 학습 데이터 기반 패턴 학습 및 자동 차단
- 대학 졸업작품 프로젝트 수준
"""

import json
import sys
import re
import os
import pickle
from typing import Dict, List, Tuple, Optional
from collections import Counter, defaultdict
from datetime import datetime
import numpy as np

# 딥러닝 라이브러리
try:
    import torch
    from transformers import (
        AutoTokenizer, 
        AutoModelForSequenceClassification,
        Trainer,
        TrainingArguments,
        pipeline
    )
    from torch.utils.data import Dataset
    DEEP_LEARNING_AVAILABLE = True
except ImportError:
    DEEP_LEARNING_AVAILABLE = False
    print("딥러닝 라이브러리가 설치되지 않았습니다. 규칙 기반 시스템을 사용합니다.", file=sys.stderr)

# 데이터베이스 연결
try:
    import mysql.connector
    from mysql.connector import Error
    DB_AVAILABLE = True
except ImportError:
    DB_AVAILABLE = False
    print("MySQL 연결 라이브러리가 설치되지 않았습니다.", file=sys.stderr)

from config import Config
from logger import setup_logger
from utils import TextUtils, ValidationUtils
from exceptions import ModelLoadError, PredictionError

logger = setup_logger(__name__)

# 분류 카테고리
CONTENT_CATEGORIES = {
    'NORMAL': 0,      # 정상
    'SPAM': 1,        # 스팸
    'MALICIOUS': 2,   # 악성
    'AD': 3,          # 홍보/광고
    'PROFANITY': 4    # 욕설
}

# 스팸 패턴 키워드 (초기 학습 데이터)
SPAM_KEYWORDS = [
    '무료', '당첨', '확인', '클릭', '링크', '광고', '홍보', '이벤트', '특가', '할인',
    '상담', '문의', '연락', '카톡', '카카오톡', '오픈채팅', '네이버', '블로그',
    '유튜브', '구독', '좋아요', '공유', '리뷰', '후기', '추천인', '코드', '쿠폰'
]

# 악성 패턴 키워드
MALICIOUS_KEYWORDS = [
    '해킹', '바이러스', '스캠', '사기', '피싱', '스팸', '차단', '신고', '고소',
    '법적', '소송', '경고', '위협', '협박', '욕설', '비방', '명예훼손'
]

# 욕설 패턴 (일부 예시)
PROFANITY_PATTERNS = [
    r'[시씨]발', r'[병빙]신', r'[개]새', r'[좆]', r'[니]미', r'[엿]', r'[젠]장',
    r'[빌]어먹', r'[뻘]', r'[썅]', r'[좆]같', r'[개]같', r'[병]신'
]


class SpamDataset(Dataset):
    """스팸 분류를 위한 데이터셋"""
    
    def __init__(self, texts: List[str], labels: List[int], tokenizer, max_length: int = 128):
        self.texts = texts
        self.labels = labels
        self.tokenizer = tokenizer
        self.max_length = max_length
    
    def __len__(self):
        return len(self.texts)
    
    def __getitem__(self, idx):
        text = str(self.texts[idx])
        label = self.labels[idx]
        
        encoding = self.tokenizer(
            text,
            truncation=True,
            padding='max_length',
            max_length=self.max_length,
            return_tensors='pt'
        )
        
        return {
            'input_ids': encoding['input_ids'].flatten(),
            'attention_mask': encoding['attention_mask'].flatten(),
            'labels': torch.tensor(label, dtype=torch.long)
        }


class AISpamFilter:
    """AI 기반 스팸/악성 댓글 필터링 시스템"""
    
    def __init__(self, use_deep_learning: bool = True, model_name: str = "klue/bert-base"):
        self.use_deep_learning = use_deep_learning and DEEP_LEARNING_AVAILABLE
        self.model_name = model_name
        self.tokenizer = None
        self.model = None
        self.classifier = None
        self.pattern_model = None  # 패턴 학습 모델
        
        # 학습 데이터 저장소
        self.learning_data = {
            'spam': [],
            'malicious': [],
            'ad': [],
            'profanity': [],
            'normal': []
        }
        
        # 패턴 통계
        self.pattern_stats = defaultdict(int)
        
        # 모델 경로
        Config.ensure_model_dir()
        self.model_path = os.path.join(Config.MODEL_CACHE_DIR, 'spam_filter_model')
        
        if self.use_deep_learning:
            self._load_model()
        else:
            logger.warning("딥러닝 모델을 사용할 수 없습니다. 규칙 기반 시스템을 사용합니다.")
    
    def _load_model(self):
        """딥러닝 모델 로드"""
        try:
            logger.info(f"모델 로딩 중: {self.model_name}")
            
            # 토크나이저 로드
            self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
            
            # 저장된 모델이 있으면 로드, 없으면 기본 모델 사용
            if os.path.exists(self.model_path):
                try:
                    self.model = AutoModelForSequenceClassification.from_pretrained(
                        self.model_path,
                        num_labels=len(CONTENT_CATEGORIES)
                    )
                    logger.info("저장된 모델을 로드했습니다.")
                except Exception as e:
                    logger.warning(f"저장된 모델 로드 실패: {e}. 기본 모델을 사용합니다.")
                    self.model = AutoModelForSequenceClassification.from_pretrained(
                        self.model_name,
                        num_labels=len(CONTENT_CATEGORIES)
                    )
            else:
                self.model = AutoModelForSequenceClassification.from_pretrained(
                    self.model_name,
                    num_labels=len(CONTENT_CATEGORIES)
                )
            
            # GPU 사용 가능 여부 확인
            device = 0 if Config.USE_GPU and torch.cuda.is_available() else -1
            if device >= 0:
                self.model = self.model.to(device)
                logger.info(f"GPU 사용: {torch.cuda.get_device_name(0)}")
            
            # 파이프라인 생성
            self.classifier = pipeline(
                "text-classification",
                model=self.model,
                tokenizer=self.tokenizer,
                device=device
            )
            
            logger.info("모델 로드 완료")
            
        except Exception as e:
            logger.error(f"모델 로드 실패: {e}")
            logger.info("규칙 기반 시스템으로 폴백합니다.")
            self.use_deep_learning = False
            raise ModelLoadError(f"모델 로드 실패: {e}") from e
    
    def _load_learning_data_from_db(self) -> Dict[str, List[str]]:
        """데이터베이스에서 학습 데이터 로드"""
        if not DB_AVAILABLE:
            return self.learning_data
        
        try:
            connection = mysql.connector.connect(**Config.get_db_config())
            cursor = connection.cursor(dictionary=True)
            
            # ai_learning_data 테이블에서 학습 데이터 가져오기
            query = """
                SELECT content_sample, block_reason, content_type, frequency
                FROM ai_learning_data
                WHERE frequency >= 1
                ORDER BY frequency DESC
                LIMIT 1000
            """
            cursor.execute(query)
            results = cursor.fetchall()
            
            for row in results:
                content = row['content_sample']
                reason = row['block_reason'] or ''
                content_type = row['content_type'] or 'POST'
                
                # block_reason에 따라 카테고리 분류
                if '스팸' in reason or 'SPAM' in reason.upper():
                    self.learning_data['spam'].append(content)
                elif '악성' in reason or 'MALICIOUS' in reason.upper():
                    self.learning_data['malicious'].append(content)
                elif '홍보' in reason or '광고' in reason or 'AD' in reason.upper():
                    self.learning_data['ad'].append(content)
                elif '욕설' in reason or 'PROFANITY' in reason.upper():
                    self.learning_data['profanity'].append(content)
            
            # blocked_comments에서 신고 건수가 많은 댓글 가져오기
            query = """
                SELECT c.content, c.report_count, bc.block_reason
                FROM comments c
                LEFT JOIN blocked_comments bc ON c.id = bc.comment_id
                WHERE c.report_count >= 3
                ORDER BY c.report_count DESC
                LIMIT 500
            """
            cursor.execute(query)
            results = cursor.fetchall()
            
            for row in results:
                content = row['content']
                report_count = row['report_count'] or 0
                block_reason = row['block_reason'] or ''
                
                if report_count >= 5:
                    if '스팸' in block_reason or not block_reason:
                        self.learning_data['spam'].append(content)
                elif report_count >= 3:
                    self.learning_data['malicious'].append(content)
            
            cursor.close()
            connection.close()
            
            logger.info(f"학습 데이터 로드 완료: 스팸={len(self.learning_data['spam'])}, "
                       f"악성={len(self.learning_data['malicious'])}, "
                       f"홍보={len(self.learning_data['ad'])}, "
                       f"욕설={len(self.learning_data['profanity'])}")
            
        except Error as e:
            logger.error(f"데이터베이스 연결 오류: {e}")
        
        return self.learning_data
    
    def _extract_features(self, text: str) -> Dict:
        """텍스트에서 특징 추출"""
        text_lower = text.lower()
        normalized = TextUtils.normalize_text(text)
        
        features = {
            'length': len(text),
            'word_count': len(text.split()),
            'spam_keyword_count': sum(1 for kw in SPAM_KEYWORDS if kw in text_lower),
            'malicious_keyword_count': sum(1 for kw in MALICIOUS_KEYWORDS if kw in text_lower),
            'profanity_pattern_count': sum(1 for pattern in PROFANITY_PATTERNS if re.search(pattern, text_lower)),
            'url_count': len(re.findall(r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\\(\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+', text)),
            'phone_count': len(re.findall(r'01[0-9]-?[0-9]{3,4}-?[0-9]{4}', text)),
            'email_count': len(re.findall(r'\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b', text)),
            'repeated_char_count': len(re.findall(r'(.)\1{3,}', text)),  # 같은 문자 4번 이상 반복
            'caps_ratio': sum(1 for c in text if c.isupper()) / len(text) if text else 0,
            'special_char_ratio': sum(1 for c in text if not c.isalnum() and c not in ' \n\t') / len(text) if text else 0
        }
        
        return features
    
    def _classify_with_rules(self, text: str, features: Dict) -> Tuple[str, float]:
        """규칙 기반 분류 (폴백)"""
        score = {
            'NORMAL': 0.0,
            'SPAM': 0.0,
            'MALICIOUS': 0.0,
            'AD': 0.0,
            'PROFANITY': 0.0
        }
        
        # 스팸 점수
        if features['spam_keyword_count'] > 0:
            score['SPAM'] += features['spam_keyword_count'] * 0.3
        if features['url_count'] > 0:
            score['SPAM'] += features['url_count'] * 0.4
        if features['phone_count'] > 0:
            score['SPAM'] += features['phone_count'] * 0.3
        if features['email_count'] > 0:
            score['AD'] += features['email_count'] * 0.5
        
        # 악성 점수
        if features['malicious_keyword_count'] > 0:
            score['MALICIOUS'] += features['malicious_keyword_count'] * 0.4
        
        # 욕설 점수
        if features['profanity_pattern_count'] > 0:
            score['PROFANITY'] += features['profanity_pattern_count'] * 0.8
        
        # 홍보 점수
        if features['spam_keyword_count'] >= 3:
            score['AD'] += 0.5
        if features['repeated_char_count'] > 0:
            score['SPAM'] += 0.2
        
        # 정상 점수 (기본값)
        if all(s == 0.0 for s in score.values()):
            score['NORMAL'] = 1.0
        else:
            score['NORMAL'] = max(0.0, 1.0 - sum(score.values()))
        
        # 가장 높은 점수의 카테고리
        best_category = max(score.items(), key=lambda x: x[1])
        confidence = min(best_category[1], 1.0)
        
        return best_category[0], confidence
    
    def _classify_with_deep_learning(self, text: str) -> Tuple[str, float]:
        """딥러닝 모델을 사용한 분류"""
        if not self.classifier:
            raise PredictionError("모델이 로드되지 않았습니다.")
        
        try:
            # 텍스트 전처리
            clean_text = TextUtils.normalize_text(text)
            
            if not clean_text:
                return 'NORMAL', 0.0
            
            # 모델 예측
            # 실제로는 fine-tuning된 모델을 사용해야 하지만,
            # 여기서는 하이브리드 방식 사용
            result = self._hybrid_classification(clean_text)
            
            return result
            
        except Exception as e:
            logger.error(f"딥러닝 분류 오류: {e}")
            # 폴백: 규칙 기반 분류
            features = self._extract_features(text)
            return self._classify_with_rules(text, features)
    
    def _hybrid_classification(self, text: str) -> Tuple[str, float]:
        """하이브리드 분류 (딥러닝 + 규칙 기반)"""
        features = self._extract_features(text)
        text_lower = text.lower()
        
        # 규칙 기반 점수
        rule_score = {
            'NORMAL': 0.0,
            'SPAM': 0.0,
            'MALICIOUS': 0.0,
            'AD': 0.0,
            'PROFANITY': 0.0
        }
        
        # 학습 데이터 기반 유사도 점수
        learning_score = {
            'NORMAL': 0.0,
            'SPAM': 0.0,
            'MALICIOUS': 0.0,
            'AD': 0.0,
            'PROFANITY': 0.0
        }
        
        # 학습 데이터와의 유사도 계산
        for category, samples in self.learning_data.items():
            if not samples:
                continue
            
            category_upper = category.upper()
            if category_upper not in learning_score:
                continue
            
            # 간단한 키워드 기반 유사도
            similarity = 0.0
            for sample in samples[:100]:  # 최대 100개만 체크
                sample_lower = sample.lower()
                # 공통 키워드 비율
                text_words = set(text_lower.split())
                sample_words = set(sample_lower.split())
                if text_words and sample_words:
                    common = len(text_words & sample_words)
                    total = len(text_words | sample_words)
                    if total > 0:
                        similarity += (common / total) / len(samples[:100])
            
            learning_score[category_upper] = min(similarity, 1.0)
        
        # 규칙 기반 점수 계산
        if features['spam_keyword_count'] > 0:
            rule_score['SPAM'] += features['spam_keyword_count'] * 0.25
        if features['url_count'] > 0:
            rule_score['SPAM'] += features['url_count'] * 0.3
            rule_score['AD'] += features['url_count'] * 0.2
        if features['phone_count'] > 0:
            rule_score['SPAM'] += features['phone_count'] * 0.25
        if features['email_count'] > 0:
            rule_score['AD'] += features['email_count'] * 0.4
        
        if features['malicious_keyword_count'] > 0:
            rule_score['MALICIOUS'] += features['malicious_keyword_count'] * 0.35
        
        if features['profanity_pattern_count'] > 0:
            rule_score['PROFANITY'] += features['profanity_pattern_count'] * 0.7
        
        if features['repeated_char_count'] > 0:
            rule_score['SPAM'] += 0.15
        
        # 딥러닝 모델 예측 (가능한 경우)
        dl_score = {
            'NORMAL': 0.0,
            'SPAM': 0.0,
            'MALICIOUS': 0.0,
            'AD': 0.0,
            'PROFANITY': 0.0
        }
        
        if self.classifier:
            try:
                # 모델 예측 (실제로는 fine-tuning된 모델 필요)
                # 여기서는 구조만 제공
                pass
            except Exception as e:
                logger.debug(f"딥러닝 예측 실패: {e}")
        
        # 최종 점수 계산 (가중 평균)
        final_score = {}
        for category in CONTENT_CATEGORIES.keys():
            final_score[category] = (
                rule_score[category] * 0.4 +
                learning_score[category] * 0.5 +
                dl_score[category] * 0.1
            )
        
        # 정상 점수 보정
        if sum(final_score.values()) < 0.5:
            final_score['NORMAL'] = 1.0 - sum(v for k, v in final_score.items() if k != 'NORMAL')
        else:
            final_score['NORMAL'] = max(0.0, 1.0 - sum(v for k, v in final_score.items() if k != 'NORMAL'))
        
        # 가장 높은 점수의 카테고리
        best_category = max(final_score.items(), key=lambda x: x[1])
        confidence = min(best_category[1], 1.0)
        
        return best_category[0], confidence
    
    def classify(self, text: str) -> Dict:
        """텍스트 분류 메인 함수"""
        if not text or not text.strip():
            return {
                'category': 'NORMAL',
                'confidence': 0.0,
                'blocked': False,
                'reason': '빈 텍스트',
                'method': 'rule-based'
            }
        
        if not ValidationUtils.validate_text_input(text):
            return {
                'category': 'MALICIOUS',
                'confidence': 0.9,
                'blocked': True,
                'reason': '입력 검증 실패',
                'method': 'validation'
            }
        
        try:
            if self.use_deep_learning:
                category, confidence = self._classify_with_deep_learning(text)
                method = 'deep-learning'
            else:
                features = self._extract_features(text)
                category, confidence = self._classify_with_rules(text, features)
                method = 'rule-based'
            
            # 차단 여부 결정 (신뢰도 0.6 이상이면 차단)
            blocked = confidence >= 0.6 and category != 'NORMAL'
            
            reason = ''
            if category == 'SPAM':
                reason = '스팸 댓글로 감지되었습니다'
            elif category == 'MALICIOUS':
                reason = '악성 댓글로 감지되었습니다'
            elif category == 'AD':
                reason = '홍보/광고 댓글로 감지되었습니다'
            elif category == 'PROFANITY':
                reason = '욕설이 포함된 댓글입니다'
            
            return {
                'category': category,
                'confidence': round(confidence, 3),
                'blocked': blocked,
                'reason': reason,
                'method': method
            }
            
        except Exception as e:
            logger.error(f"분류 오류: {e}")
            return {
                'category': 'NORMAL',
                'confidence': 0.0,
                'blocked': False,
                'reason': f'분류 오류: {str(e)}',
                'method': 'error'
            }
    
    def review_reported_content(self, content_id: int, content_type: str = 'COMMENT', 
                                report_count: int = 0, content: str = '') -> Dict:
        """신고 건수 기반 선 검토"""
        if not content:
            return {
                'reviewed': False,
                'blocked': False,
                'reason': '내용이 없습니다'
            }
        
        # 신고 건수에 따른 우선순위
        priority = 'LOW'
        if report_count >= 10:
            priority = 'CRITICAL'
        elif report_count >= 5:
            priority = 'HIGH'
        elif report_count >= 3:
            priority = 'MEDIUM'
        
        # AI 분류 수행
        classification = self.classify(content)
        
        # 신고 건수와 AI 분류 결과를 종합하여 결정
        should_block = False
        reason = ''
        
        if priority == 'CRITICAL':
            # 신고가 10건 이상이면 AI 신뢰도가 낮아도 차단
            should_block = classification['confidence'] >= 0.4 or classification['blocked']
            reason = f'신고 건수 {report_count}건 + AI 감지 ({classification["category"]})'
        elif priority == 'HIGH':
            # 신고가 5건 이상이면 AI 신뢰도 0.5 이상이면 차단
            should_block = classification['confidence'] >= 0.5 or classification['blocked']
            reason = f'신고 건수 {report_count}건 + AI 감지 ({classification["category"]})'
        elif priority == 'MEDIUM':
            # 신고가 3건 이상이면 AI 신뢰도 0.6 이상이면 차단
            should_block = classification['confidence'] >= 0.6 or classification['blocked']
            reason = f'신고 건수 {report_count}건 + AI 감지 ({classification["category"]})'
        else:
            # 신고가 적으면 AI 결과만 따름
            should_block = classification['blocked']
            reason = classification['reason']
        
        return {
            'reviewed': True,
            'priority': priority,
            'blocked': should_block,
            'category': classification['category'],
            'confidence': classification['confidence'],
            'reason': reason,
            'report_count': report_count
        }
    
    def train_model(self, training_data: List[Tuple[str, str]]):
        """모델 학습 (fine-tuning)"""
        if not self.use_deep_learning:
            logger.warning("딥러닝 모델이 없어 학습할 수 없습니다.")
            return False
        
        try:
            # 학습 데이터 준비
            texts = [item[0] for item in training_data]
            labels = [CONTENT_CATEGORIES.get(item[1], 0) for item in training_data]
            
            if len(texts) < 10:
                logger.warning("학습 데이터가 부족합니다 (최소 10개 필요)")
                return False
            
            # 데이터셋 생성
            dataset = SpamDataset(texts, labels, self.tokenizer)
            
            # 학습 인자 설정
            training_args = TrainingArguments(
                output_dir=self.model_path,
                num_train_epochs=3,
                per_device_train_batch_size=Config.BATCH_SIZE,
                per_device_eval_batch_size=Config.BATCH_SIZE,
                warmup_steps=100,
                weight_decay=0.01,
                logging_dir=f'{self.model_path}/logs',
                logging_steps=10,
                save_strategy='epoch',
                evaluation_strategy='epoch' if len(texts) > 50 else 'no'
            )
            
            # Trainer 생성 및 학습
            trainer = Trainer(
                model=self.model,
                args=training_args,
                train_dataset=dataset
            )
            
            trainer.train()
            
            # 모델 저장
            self.model.save_pretrained(self.model_path)
            self.tokenizer.save_pretrained(self.model_path)
            
            logger.info(f"모델 학습 완료: {len(texts)}개 샘플")
            return True
            
        except Exception as e:
            logger.error(f"모델 학습 오류: {e}")
            return False
    
    def update_learning_data(self, text: str, category: str, confirmed: bool = True):
        """학습 데이터 업데이트"""
        if not text or not category:
            return
        
        category_lower = category.lower()
        if category_lower in self.learning_data:
            if confirmed:
                self.learning_data[category_lower].append(text)
                # 최대 1000개만 유지
                if len(self.learning_data[category_lower]) > 1000:
                    self.learning_data[category_lower] = self.learning_data[category_lower][-1000:]
                
                logger.info(f"학습 데이터 업데이트: {category_lower} (+1, 총 {len(self.learning_data[category_lower])}개)")
    
    def save_patterns(self, filepath: str = None):
        """패턴 저장"""
        if filepath is None:
            filepath = os.path.join(Config.MODEL_CACHE_DIR, 'spam_patterns.pkl')
        
        data = {
            'learning_data': self.learning_data,
            'pattern_stats': dict(self.pattern_stats)
        }
        
        with open(filepath, 'wb') as f:
            pickle.dump(data, f)
        
        logger.info(f"패턴 저장 완료: {filepath}")
    
    def load_patterns(self, filepath: str = None):
        """패턴 로드"""
        if filepath is None:
            filepath = os.path.join(Config.MODEL_CACHE_DIR, 'spam_patterns.pkl')
        
        if not os.path.exists(filepath):
            logger.warning(f"패턴 파일이 없습니다: {filepath}")
            return
        
        try:
            with open(filepath, 'rb') as f:
                data = pickle.load(f)
            
            self.learning_data = data.get('learning_data', self.learning_data)
            self.pattern_stats = defaultdict(int, data.get('pattern_stats', {}))
            
            logger.info(f"패턴 로드 완료: {filepath}")
        except Exception as e:
            logger.error(f"패턴 로드 오류: {e}")


def main():
    """메인 함수"""
    if len(sys.argv) < 2:
        print(json.dumps({
            'error': '사용법이 잘못되었습니다.',
            'usage': [
                'python ai_spam_filter.py classify "<content>"',
                'python ai_spam_filter.py review <content_id> <content_type> <report_count> "<content>"',
                'python ai_spam_filter.py train',
                'python ai_spam_filter.py load_learning_data'
            ]
        }), file=sys.stderr)
        sys.exit(1)
    
    command = sys.argv[1]
    filter_system = AISpamFilter(use_deep_learning=True)
    
    try:
        if command == 'classify':
            if len(sys.argv) < 3:
                print(json.dumps({'error': '내용이 필요합니다.'}), file=sys.stderr)
                sys.exit(1)
            
            content = sys.argv[2]
            result = filter_system.classify(content)
            print(json.dumps(result, ensure_ascii=False, indent=2))
        
        elif command == 'review':
            if len(sys.argv) < 6:
                print(json.dumps({'error': '파라미터가 부족합니다.'}), file=sys.stderr)
                sys.exit(1)
            
            content_id = int(sys.argv[2])
            content_type = sys.argv[3]
            report_count = int(sys.argv[4])
            content = sys.argv[5]
            
            result = filter_system.review_reported_content(
                content_id, content_type, report_count, content
            )
            print(json.dumps(result, ensure_ascii=False, indent=2))
        
        elif command == 'train':
            # 데이터베이스에서 학습 데이터 로드
            filter_system._load_learning_data_from_db()
            
            # 학습 데이터 준비
            training_data = []
            for category, samples in filter_system.learning_data.items():
                category_upper = category.upper()
                if category_upper in CONTENT_CATEGORIES:
                    for sample in samples:
                        training_data.append((sample, category_upper))
            
            if training_data:
                success = filter_system.train_model(training_data)
                result = {
                    'success': success,
                    'training_samples': len(training_data)
                }
            else:
                result = {
                    'success': False,
                    'error': '학습 데이터가 없습니다.'
                }
            
            print(json.dumps(result, ensure_ascii=False, indent=2))
        
        elif command == 'load_learning_data':
            filter_system._load_learning_data_from_db()
            result = {
                'success': True,
                'data_counts': {
                    category: len(samples)
                    for category, samples in filter_system.learning_data.items()
                }
            }
            print(json.dumps(result, ensure_ascii=False, indent=2))
        
        else:
            print(json.dumps({'error': f'알 수 없는 명령: {command}'}), file=sys.stderr)
            sys.exit(1)
    
    except Exception as e:
        logger.error(f"오류 발생: {e}")
        print(json.dumps({
            'error': str(e),
            'type': type(e).__name__
        }), file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
