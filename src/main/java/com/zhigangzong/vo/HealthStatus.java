package com.zhigangzong.vo;

public record HealthStatus(String status, String database, String databaseVersion, int tableCount) {
}
