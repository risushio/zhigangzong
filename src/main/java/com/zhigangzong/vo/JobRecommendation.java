package com.zhigangzong.vo;

import java.math.BigDecimal;
import java.util.List;

/** score 表示适配程度，不是录用概率；仅预留接口契约。 */
public record JobRecommendation(long jobId, BigDecimal score, List<String> matchedReasons,
                                List<String> missingSkills, List<String> conflicts,
                                List<String> missingInformation) {
}
