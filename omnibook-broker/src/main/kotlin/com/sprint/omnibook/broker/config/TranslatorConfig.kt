package com.sprint.omnibook.broker.config

import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.translator.AirbnbTranslator
import com.sprint.omnibook.broker.translator.PayloadTranslator
import com.sprint.omnibook.broker.translator.YanoljaTranslator
import com.sprint.omnibook.broker.translator.YeogieottaeTranslator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.EnumMap

/**
 * Translator Map 빈 설정.
 * 각 Translator는 @Component로 등록되어 있으며, 여기서는 Map으로 묶어서 제공한다.
 */
@Configuration
class TranslatorConfig(
    private val yanoljaTranslator: YanoljaTranslator,
    private val airbnbTranslator: AirbnbTranslator,
    private val yeogieottaeTranslator: YeogieottaeTranslator
) {

    @Bean
    fun translators(): Map<PlatformType, PayloadTranslator> {
        val map = EnumMap<PlatformType, PayloadTranslator>(PlatformType::class.java)
        map[PlatformType.YANOLJA] = yanoljaTranslator
        map[PlatformType.AIRBNB] = airbnbTranslator
        map[PlatformType.YEOGIEOTTAE] = yeogieottaeTranslator
        return map
    }
}
