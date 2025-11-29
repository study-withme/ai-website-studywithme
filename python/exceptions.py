#!/usr/bin/env python3
"""
커스텀 예외 클래스
"""


class AIRecommendationError(Exception):
    """추천 시스템 기본 예외"""
    pass


class DatabaseConnectionError(AIRecommendationError):
    """데이터베이스 연결 오류"""
    pass


class InvalidInputError(AIRecommendationError):
    """잘못된 입력 오류"""
    pass


class ModelLoadError(AIRecommendationError):
    """모델 로드 오류"""
    pass


class PredictionError(AIRecommendationError):
    """예측 오류"""
    pass


