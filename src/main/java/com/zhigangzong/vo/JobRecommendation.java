package com.zhigangzong.vo;

import java.math.BigDecimal;
import java.util.List;

/** score 是 0–100 的规则适配分，不是录用概率。 */
public record JobRecommendation(long jobId, BigDecimal score, List<String> matchedReasons,
                                List<String> missingSkills, List<String> conflicts,
                                List<String> missingInformation, String title, String city) {
}
