package com.sprint.omnibook.broker.config;

import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.translator.AirbnbTranslator;
import com.sprint.omnibook.broker.translator.PayloadTranslator;
import com.sprint.omnibook.broker.translator.YanoljaTranslator;
import com.sprint.omnibook.broker.translator.YeogieottaeTranslator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Translator Map 빈 설정.
 * 각 Translator는 @Component로 등록되어 있으며, 여기서는 Map으로 묶어서 제공한다.
 */
@Configuration
public class TranslatorConfig {

    @Bean
    public Map<PlatformType, PayloadTranslator> translators(
            YanoljaTranslator yanoljaTranslator,
            AirbnbTranslator airbnbTranslator,
            YeogieottaeTranslator yeogieottaeTranslator) {

        Map<PlatformType, PayloadTranslator> map = new EnumMap<>(PlatformType.class);
        map.put(PlatformType.YANOLJA, yanoljaTranslator);
        map.put(PlatformType.AIRBNB, airbnbTranslator);
        map.put(PlatformType.YEOGIEOTTAE, yeogieottaeTranslator);
        return map;
    }
}
