package com.example.demo.dto;

/** 所有 API 錯誤共用的回應格式。 */
public record ApiError(String code, String message) {
}
