#!/usr/bin/env python3
"""
설정 관리 모듈
환경 변수와 기본값을 통합 관리합니다.
"""

import os
from typing import Dict


class Config:
    """애플리케이션 설정"""
    
    # 데이터베이스 설정
    DB_HOST = os.getenv('DB_HOST', 'localhost')
    DB_PORT = int(os.getenv('DB_PORT', 3306))
    DB_USER = os.getenv('DB_USER', 'root')
    DB_PASSWORD = os.getenv('DB_PASSWORD', 'Xmflslxl2@')
    DB_NAME = os.getenv('DB_NAME', 'studywithmever2')
    
    # 로깅 설정
    LOG_LEVEL = os.getenv('LOG_LEVEL', 'INFO')
    LOG_FORMAT = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    
    # 추천 시스템 설정
    RECOMMENDATION_DAYS = int(os.getenv('RECOMMENDATION_DAYS', 30))
    DEFAULT_RECOMMENDATION_LIMIT = int(os.getenv('DEFAULT_RECOMMENDATION_LIMIT', 20))
    
    # 요약 시스템 설정
    DEFAULT_SUMMARY_LENGTH = int(os.getenv('DEFAULT_SUMMARY_LENGTH', 200))
    MAX_SUMMARY_LENGTH = int(os.getenv('MAX_SUMMARY_LENGTH', 500))
    
    # 딥러닝 모델 설정
    MODEL_CACHE_DIR = os.getenv('MODEL_CACHE_DIR', './models')
    USE_GPU = os.getenv('USE_GPU', 'false').lower() == 'true'
    BATCH_SIZE = int(os.getenv('BATCH_SIZE', 16))
    
    @classmethod
    def get_db_config(cls) -> Dict:
        """데이터베이스 연결 설정 반환"""
        return {
            'host': cls.DB_HOST,
            'port': cls.DB_PORT,
            'user': cls.DB_USER,
            'password': cls.DB_PASSWORD,
            'database': cls.DB_NAME,
            'charset': 'utf8mb4'
        }
    
    @classmethod
    def ensure_model_dir(cls):
        """모델 디렉토리 생성"""
        os.makedirs(cls.MODEL_CACHE_DIR, exist_ok=True)


