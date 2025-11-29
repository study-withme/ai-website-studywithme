#!/usr/bin/env python3
"""
성능 평가 지표 계산 모듈
"""

from typing import List, Dict, Tuple
from collections import defaultdict
import math


class RecommendationMetrics:
    """추천 시스템 성능 평가 지표"""
    
    @staticmethod
    def precision_at_k(recommended: List[int], relevant: List[int], k: int) -> float:
        """
        Precision@K 계산
        
        Args:
            recommended: 추천된 아이템 ID 리스트
            relevant: 실제 관련 있는 아이템 ID 리스트
            k: 상위 K개
        
        Returns:
            Precision@K 값 (0.0 ~ 1.0)
        """
        if k == 0 or not recommended:
            return 0.0
        
        recommended_set = set(recommended[:k])
        relevant_set = set(relevant)
        
        if not relevant_set:
            return 0.0
        
        intersection = recommended_set & relevant_set
        return len(intersection) / min(k, len(recommended))
    
    @staticmethod
    def recall_at_k(recommended: List[int], relevant: List[int], k: int) -> float:
        """
        Recall@K 계산
        
        Args:
            recommended: 추천된 아이템 ID 리스트
            relevant: 실제 관련 있는 아이템 ID 리스트
            k: 상위 K개
        
        Returns:
            Recall@K 값 (0.0 ~ 1.0)
        """
        if not recommended or not relevant:
            return 0.0
        
        recommended_set = set(recommended[:k])
        relevant_set = set(relevant)
        
        if not relevant_set:
            return 0.0
        
        intersection = recommended_set & relevant_set
        return len(intersection) / len(relevant_set)
    
    @staticmethod
    def f1_score_at_k(recommended: List[int], relevant: List[int], k: int) -> float:
        """
        F1-Score@K 계산
        
        Args:
            recommended: 추천된 아이템 ID 리스트
            relevant: 실제 관련 있는 아이템 ID 리스트
            k: 상위 K개
        
        Returns:
            F1-Score@K 값 (0.0 ~ 1.0)
        """
        precision = RecommendationMetrics.precision_at_k(recommended, relevant, k)
        recall = RecommendationMetrics.recall_at_k(recommended, relevant, k)
        
        if precision + recall == 0:
            return 0.0
        
        return 2 * (precision * recall) / (precision + recall)
    
    @staticmethod
    def ndcg_at_k(recommended: List[int], relevant: List[int], k: int) -> float:
        """
        NDCG@K (Normalized Discounted Cumulative Gain) 계산
        
        Args:
            recommended: 추천된 아이템 ID 리스트
            relevant: 실제 관련 있는 아이템 ID 리스트
            k: 상위 K개
        
        Returns:
            NDCG@K 값 (0.0 ~ 1.0)
        """
        if not recommended or not relevant:
            return 0.0
        
        recommended_set = set(recommended[:k])
        relevant_set = set(relevant)
        
        if not relevant_set:
            return 0.0
        
        # DCG 계산
        dcg = 0.0
        for i, item_id in enumerate(recommended[:k], 1):
            if item_id in relevant_set:
                dcg += 1.0 / math.log2(i + 1)
        
        # IDCG 계산 (이상적인 순서)
        idcg = 0.0
        num_relevant = min(k, len(relevant_set))
        for i in range(1, num_relevant + 1):
            idcg += 1.0 / math.log2(i + 1)
        
        if idcg == 0:
            return 0.0
        
        return dcg / idcg
    
    @staticmethod
    def calculate_all_metrics(recommended: List[int], relevant: List[int], k: int = 10) -> Dict[str, float]:
        """
        모든 추천 지표 계산
        
        Args:
            recommended: 추천된 아이템 ID 리스트
            relevant: 실제 관련 있는 아이템 ID 리스트
            k: 상위 K개
        
        Returns:
            모든 지표를 포함한 딕셔너리
        """
        return {
            'precision@k': RecommendationMetrics.precision_at_k(recommended, relevant, k),
            'recall@k': RecommendationMetrics.recall_at_k(recommended, relevant, k),
            'f1_score@k': RecommendationMetrics.f1_score_at_k(recommended, relevant, k),
            'ndcg@k': RecommendationMetrics.ndcg_at_k(recommended, relevant, k)
        }


class ClassificationMetrics:
    """분류 시스템 성능 평가 지표"""
    
    @staticmethod
    def accuracy(y_true: List[str], y_pred: List[str]) -> float:
        """정확도 계산"""
        if len(y_true) != len(y_pred):
            return 0.0
        
        correct = sum(1 for true, pred in zip(y_true, y_pred) if true == pred)
        return correct / len(y_true) if y_true else 0.0
    
    @staticmethod
    def precision(y_true: List[str], y_pred: List[str], label: str) -> float:
        """특정 라벨에 대한 Precision 계산"""
        tp = sum(1 for true, pred in zip(y_true, y_pred) if true == label and pred == label)
        fp = sum(1 for true, pred in zip(y_true, y_pred) if true != label and pred == label)
        
        if tp + fp == 0:
            return 0.0
        
        return tp / (tp + fp)
    
    @staticmethod
    def recall(y_true: List[str], y_pred: List[str], label: str) -> float:
        """특정 라벨에 대한 Recall 계산"""
        tp = sum(1 for true, pred in zip(y_true, y_pred) if true == label and pred == label)
        fn = sum(1 for true, pred in zip(y_true, y_pred) if true == label and pred != label)
        
        if tp + fn == 0:
            return 0.0
        
        return tp / (tp + fn)
    
    @staticmethod
    def f1_score(y_true: List[str], y_pred: List[str], label: str) -> float:
        """특정 라벨에 대한 F1-Score 계산"""
        precision = ClassificationMetrics.precision(y_true, y_pred, label)
        recall = ClassificationMetrics.recall(y_true, y_pred, label)
        
        if precision + recall == 0:
            return 0.0
        
        return 2 * (precision * recall) / (precision + recall)
    
    @staticmethod
    def calculate_all_metrics(y_true: List[str], y_pred: List[str], labels: List[str]) -> Dict[str, Dict[str, float]]:
        """
        모든 분류 지표 계산 (각 라벨별)
        
        Args:
            y_true: 실제 라벨 리스트
            y_pred: 예측 라벨 리스트
            labels: 평가할 라벨 리스트
        
        Returns:
            라벨별 지표를 포함한 딕셔너리
        """
        results = {}
        
        for label in labels:
            results[label] = {
                'precision': ClassificationMetrics.precision(y_true, y_pred, label),
                'recall': ClassificationMetrics.recall(y_true, y_pred, label),
                'f1_score': ClassificationMetrics.f1_score(y_true, y_pred, label)
            }
        
        # 전체 정확도
        results['overall'] = {
            'accuracy': ClassificationMetrics.accuracy(y_true, y_pred)
        }
        
        return results


class SummaryMetrics:
    """요약 시스템 성능 평가 지표"""
    
    @staticmethod
    def rouge_l_score(reference: str, summary: str) -> float:
        """
        ROUGE-L Score 계산 (간단한 버전)
        
        Args:
            reference: 참조 요약
            summary: 생성된 요약
        
        Returns:
            ROUGE-L Score (0.0 ~ 1.0)
        """
        if not reference or not summary:
            return 0.0
        
        ref_words = set(reference.split())
        sum_words = set(summary.split())
        
        if not ref_words or not sum_words:
            return 0.0
        
        intersection = ref_words & sum_words
        union = ref_words | sum_words
        
        if not union:
            return 0.0
        
        return len(intersection) / len(union)
    
    @staticmethod
    def compression_ratio(original: str, summary: str) -> float:
        """압축률 계산"""
        if not original:
            return 0.0
        
        return len(summary) / len(original) if original else 0.0
    
    @staticmethod
    def calculate_all_metrics(reference: str, summary: str, original: str) -> Dict[str, float]:
        """
        모든 요약 지표 계산
        
        Args:
            reference: 참조 요약 (실제 요약)
            summary: 생성된 요약
            original: 원본 텍스트
        
        Returns:
            모든 지표를 포함한 딕셔너리
        """
        return {
            'rouge_l': SummaryMetrics.rouge_l_score(reference, summary),
            'compression_ratio': SummaryMetrics.compression_ratio(original, summary),
            'summary_length': len(summary),
            'original_length': len(original)
        }


