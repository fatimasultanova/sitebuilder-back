-- V3__seed_templates.sql
-- Generated from src/models/siteTemplate.js (13 templates). Re-runnable: upserts by slug.
-- theme_config.templateKey is what the frontend uses to match a row to its local template
-- (src/models/templateCatalog.js, matching rule 1).

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('saas-starter', 'SaaS Starter', 'saas', 'A clean, modern SaaS layout: hero, logo cloud, features, a product preview, pricing, testimonials, FAQ and a call to action.', false, 10,
        '{"templateKey":"saas-starter","themePreset":"saas"}'::jsonb,
        '{"az":{"name":"SaaS Starter","description":"Təmiz və müasir SaaS düzülüşü: qəhrəman, loqo zolağı, xüsusiyyətlər, məhsul önizləməsi, qiymətlər, rəylər, FAQ və çağırış."}}'::jsonb,
        '{"layout":"landing","themePreset":"saas","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["hero","logoCloud","featureGrid","productPreview","pricing","testimonials","faq","cta","footer"]},{"id":"pricing","name":{"en":"Pricing","az":"Qiymətlər"},"sections":["hero","pricing","faq","footer"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contact","footer"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('portfolio-minimal', 'Portfolio Minimal', 'portfolio', 'A minimal personal portfolio built on strong typography and generous spacing: editorial hero, selected work, services and a contact page.', false, 20,
        '{"templateKey":"portfolio-minimal","themePreset":"atelier"}'::jsonb,
        '{"az":{"name":"Portfolio Minimal","description":"Güclü tipoqrafiya və geniş boşluqlar üzərində qurulmuş minimal şəxsi portfolio: redaksiya qəhrəmanı, seçilmiş işlər, xidmətlər və əlaqə səhifəsi."}}'::jsonb,
        '{"layout":"split","themePreset":"atelier","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["editorialHero","projects","services","about","testimonials","contactCta","footerColumns"]},{"id":"work","name":{"en":"Work","az":"İşlər"},"sections":["editorialHero","projects","services","footerColumns"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["about","services","testimonials","footerColumns"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footerColumns"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('nova-ai', 'Nova AI', 'ai', 'A futuristic premium AI product site: animated hero, bento features, a workflow, integrations, metrics, advanced pricing and a detailed footer.', true, 30,
        '{"templateKey":"nova-ai","themePreset":"nova"}'::jsonb,
        '{"az":{"name":"Nova AI","description":"Futuristik premium AI məhsul saytı: animasiyalı qəhrəman, bento xüsusiyyətlər, iş axını, inteqrasiyalar, göstəricilər, qabaqcıl qiymətlər və geniş alt hissə."}}'::jsonb,
        '{"layout":"landing","themePreset":"nova","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["aiHero","logoCloud","bento","productPreview","workflow","integrations","metrics","pricing","testimonials","faq","cta","footerDetailed"]},{"id":"features","name":{"en":"Features","az":"Xüsusiyyətlər"},"sections":["hero","bento","featureGrid","workflow","integrations","cta","footerDetailed"]},{"id":"solutions","name":{"en":"Solutions","az":"Həllər"},"sections":["hero","metrics","bento","workflow","cta","footerDetailed"]},{"id":"pricing","name":{"en":"Pricing","az":"Qiymətlər"},"sections":["hero","pricing","metrics","faq","cta","footerDetailed"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["editorialHero","metrics","workflow","testimonials","footerDetailed"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footerDetailed"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('maison', 'Maison', 'realEstate', 'A luxury architecture and interiors site: oversized editorial hero, featured projects, services, statistics and calm premium motion.', true, 40,
        '{"templateKey":"maison","themePreset":"maison"}'::jsonb,
        '{"az":{"name":"Maison","description":"Lüks memarlıq və interyer saytı: böyük redaksiya qəhrəmanı, seçilmiş layihələr, xidmətlər, statistikalar və sakit premium hərəkət."}}'::jsonb,
        '{"layout":"editorial","themePreset":"maison","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["heroSplit","stats","projects","story","services","testimonials","contactCta","footerColumns"]},{"id":"projects","name":{"en":"Projects","az":"Layihələr"},"sections":["editorialHero","projects","story","footerColumns"]},{"id":"services","name":{"en":"Services","az":"Xidmətlər"},"sections":["editorialHero","services","stats","contactCta","footerColumns"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["editorialHero","story","stats","testimonials","footerColumns"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footerColumns"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('agency-clean', 'Agency Clean', 'agency', 'A modern digital agency site: asymmetrical split hero, client logos, a services grid, featured work and a numbered process.', false, 50,
        '{"templateKey":"agency-clean","themePreset":"agency"}'::jsonb,
        '{"az":{"name":"Agency Clean","description":"Müasir rəqəmsal agentlik saytı: asimmetrik bölünmüş qəhrəman, müştəri loqoları, xidmət şəbəkəsi, seçilmiş işlər və nömrələnmiş proses."}}'::jsonb,
        '{"layout":"split","themePreset":"agency","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["heroSplit","logoCloud","services","projects","workflow","testimonials","cta","footer"]},{"id":"services","name":{"en":"Services","az":"Xidmətlər"},"sections":["heroSplit","services","workflow","cta","footer"]},{"id":"work","name":{"en":"Work","az":"İşlər"},"sections":["heroSplit","projects","testimonials","footer"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footer"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('restaurant-simple', 'Restaurant Simple', 'restaurant', 'A warm restaurant or café site: an image-led hero, a short story, a menu of categories, a chef highlight, opening hours and a reservation call to action.', false, 60,
        '{"templateKey":"restaurant-simple","themePreset":"bistro"}'::jsonb,
        '{"az":{"name":"Restaurant Simple","description":"İsti restoran və ya kafe saytı: şəkil əsaslı qəhrəman, qısa hekayə, kateqoriyalı menyu, aşpaz vurğusu, iş saatları və rezervasiya çağırışı."}}'::jsonb,
        '{"layout":"editorial","themePreset":"bistro","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["heroImage","about","menu","story","hours","testimonials","contactCta","footer"]},{"id":"menu","name":{"en":"Menu","az":"Menyu"},"sections":["hero","menu","hours","contactCta","footer"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["hero","story","hours","testimonials","footer"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footer"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('vertex-studio', 'Vertex Studio', 'studio', 'A high-end design studio site: a type-led editorial opening, selected work, an asymmetric project grid, a statement, awards and six pages.', true, 70,
        '{"templateKey":"vertex-studio","themePreset":"vertex"}'::jsonb,
        '{"az":{"name":"Vertex Studio","description":"Yüksək səviyyəli dizayn studiyası saytı: tipoqrafiya əsaslı redaksiya açılışı, seçilmiş işlər, asimmetrik layihə şəbəkəsi, bəyanat, mükafatlar və altı səhifə."}}'::jsonb,
        '{"layout":"editorial","themePreset":"vertex","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["heroType","projects","projectGrid","services","statement","recognition","story","testimonials","contactCta","footerColumns"]},{"id":"work","name":{"en":"Work","az":"İşlər"},"sections":["heroSplit","projects","projectGrid","footerColumns"]},{"id":"services","name":{"en":"Services","az":"Xidmətlər"},"sections":["heroSplit","services","workflow","recognition","contactCta","footerColumns"]},{"id":"studio","name":{"en":"Studio","az":"Studiya"},"sections":["heroSplit","statement","story","recognition","footerColumns"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["heroSplit","story","stats","testimonials","footerColumns"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footerColumns"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('noir-portfolio', 'Noir Portfolio', 'portfolio', 'A dark cinematic portfolio: a full-width image hero, selected work, a featured case study, a client list and an oversized statement.', true, 80,
        '{"templateKey":"noir-portfolio","themePreset":"noir"}'::jsonb,
        '{"az":{"name":"Noir Portfolio","description":"Tünd kinematik portfolio: tam enli şəkil qəhrəmanı, seçilmiş işlər, seçilmiş layihə təhlili, müştəri siyahısı və nəhəng bəyanat."}}'::jsonb,
        '{"layout":"editorial","themePreset":"noir","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["heroImage","projects","projectGrid","story","services","logoCloud","statement","contactCta","footer"]},{"id":"projects","name":{"en":"Projects","az":"Layihələr"},"sections":["heroImage","projects","projectGrid","footer"]},{"id":"project","name":{"en":"Project","az":"Layihə"},"sections":["heroImage","story","projects","footer"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["heroImage","statement","about","testimonials","footer"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footer"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('business-starter', 'Business Starter', 'business', 'A modern company or consultancy site: a split hero, a client strip, a services grid, a numbered process, the numbers and a clear call to action.', false, 90,
        '{"templateKey":"business-starter","themePreset":"corporate"}'::jsonb,
        '{"az":{"name":"Business Starter","description":"Müasir şirkət və ya konsaltinq saytı: bölünmüş qəhrəman, müştəri zolağı, xidmət şəbəkəsi, nömrələnmiş proses, rəqəmlər və aydın çağırış."}}'::jsonb,
        '{"layout":"landing","themePreset":"corporate","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["heroSplit","logoCloud","services","workflow","stats","testimonials","contactCta","footer"]},{"id":"services","name":{"en":"Services","az":"Xidmətlər"},"sections":["heroSplit","services","workflow","stats","contactCta","footer"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["hero","stats","story","testimonials","footer"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footer"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('forge-construction', 'Forge Construction', 'construction', 'A premium construction and engineering site: an industrial image hero, featured projects, capabilities, metrics, a process, a safety and quality strip and five pages.', true, 100,
        '{"templateKey":"forge-construction","themePreset":"forge"}'::jsonb,
        '{"az":{"name":"Forge Construction","description":"Premium tikinti və mühəndislik saytı: sənaye qəhrəmanı, seçilmiş layihələr, imkanlar, göstəricilər, proses, təhlükəsizlik zolağı və beş səhifə."}}'::jsonb,
        '{"layout":"editorial","themePreset":"forge","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["heroImage","logoCloud","projects","services","stats","workflow","trust","testimonials","contactCta","footerColumns"]},{"id":"projects","name":{"en":"Projects","az":"Layihələr"},"sections":["heroSplit","projects","projectGrid","stats","contactCta","footerColumns"]},{"id":"services","name":{"en":"Services","az":"Xidmətlər"},"sections":["heroSplit","services","workflow","trust","contactCta","footerColumns"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["heroSplit","story","stats","testimonials","footerColumns"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footerColumns"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('aurelia-wellness', 'Aurelia Wellness', 'wellness', 'A luxury spa and wellness site: an image-led editorial hero, a philosophy story, a treatment list, signature services, a gallery, opening hours and five pages.', true, 110,
        '{"templateKey":"aurelia-wellness","themePreset":"aurelia"}'::jsonb,
        '{"az":{"name":"Aurelia Wellness","description":"Lüks gözəllik və spa saytı: böyük redaksiya qəhrəmanı, fəlsəfə hekayəsi, müalicə siyahısı, xidmətlər, qalereya, iş saatları və beş səhifə."}}'::jsonb,
        '{"layout":"editorial","themePreset":"aurelia","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["editorialHero","story","offering","services","projects","testimonials","hours","contactCta","footerColumns"]},{"id":"treatments","name":{"en":"Treatments","az":"Müalicələr"},"sections":["hero","offering","services","faq","contactCta","footerColumns"]},{"id":"about","name":{"en":"About","az":"Haqqımızda"},"sections":["hero","story","stats","testimonials","footerColumns"]},{"id":"booking","name":{"en":"Booking","az":"Rezervasiya"},"sections":["hero","hours","contact","footerColumns"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footerColumns"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('pulse-finance', 'Pulse Finance', 'finance', 'A modern fintech product site: a product hero, a dashboard preview, metrics, a features grid, a security panel, integrations, pricing, an FAQ and six pages.', true, 120,
        '{"templateKey":"pulse-finance","themePreset":"ledger"}'::jsonb,
        '{"az":{"name":"Pulse Finance","description":"Müasir fintech platforması: bölünmüş qəhrəman, məhsul önizləməsi, göstəricilər, xüsusiyyət şəbəkəsi, təhlükəsizlik zolağı, inteqrasiyalar, qiymətlər, FAQ və altı səhifə."}}'::jsonb,
        '{"layout":"landing","themePreset":"ledger","pages":[{"id":"home","name":{"en":"Home","az":"Ana səhifə"},"sections":["heroSplit","logoCloud","productPreview","metrics","featureGrid","trust","integrations","pricing","testimonials","faq","contactCta","footerDetailed"]},{"id":"product","name":{"en":"Product","az":"Məhsul"},"sections":["hero","productPreview","featureGrid","metrics","trust","contactCta","footerDetailed"]},{"id":"solutions","name":{"en":"Solutions","az":"Həllər"},"sections":["hero","featureGrid","integrations","metrics","testimonials","contactCta","footerDetailed"]},{"id":"pricing","name":{"en":"Pricing","az":"Qiymətlər"},"sections":["hero","pricing","metrics","faq","contactCta","footerDetailed"]},{"id":"security","name":{"en":"Security","az":"Təhlükəsizlik"},"sections":["hero","trust","featureGrid","metrics","faq","contactCta","footerDetailed"]},{"id":"contact","name":{"en":"Contact","az":"Əlaqə"},"sections":["contactCta","contact","footerDetailed"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();

INSERT INTO "sitebuilder-data".templates (slug, name, category, description, is_premium, sort_order, theme_config, translations, structure)
VALUES ('atlas-dashboard', 'Atlas Dashboard', 'dashboard', 'A premium application template: an app bar, KPI cards, chart panels, data tables, an activity feed, status panels, customer and report cards and a settings area across six pages.', true, 130,
        '{"templateKey":"atlas-dashboard","themePreset":"atlas"}'::jsonb,
        '{"az":{"name":"Atlas Dashboard","description":"Premium tətbiq şablonu: tətbiq zolağı, KPI kartları, qrafik panelləri, məlumat cədvəlləri, fəaliyyət lenti, status panelləri, müştəri və hesabat kartları və altı səhifə üzrə tənzimləmə sahəsi."}}'::jsonb,
        '{"layout":"app","themePreset":"atlas","pages":[{"id":"dashboard","name":{"en":"Dashboard","az":"İdarə paneli"},"sections":["appBar","kpiRow","chartGrid","dataTable","activityFeed","customerGrid","statusGrid","quickActions","footer"]},{"id":"analytics","name":{"en":"Analytics","az":"Analitika"},"sections":["appBar","kpiRow","chartGrid","dataTable","statusGrid","quickActions","footer"]},{"id":"customers","name":{"en":"Customers","az":"Müştərilər"},"sections":["appBar","kpiRow","dataTable","customerGrid","footer"]},{"id":"transactions","name":{"en":"Transactions","az":"Əməliyyatlar"},"sections":["appBar","kpiRow","dataTable","statusGrid","footer"]},{"id":"reports","name":{"en":"Reports","az":"Hesabatlar"},"sections":["appBar","kpiRow","chartGrid","reportGrid","quickActions","footer"]},{"id":"settings","name":{"en":"Settings","az":"Tənzimləmələr"},"sections":["appBar","settingsGroups","settingsForm","footer"]}]}'::jsonb)
    ON CONFLICT (slug) DO UPDATE SET name=EXCLUDED.name, category=EXCLUDED.category, description=EXCLUDED.description,
                              is_premium=EXCLUDED.is_premium, sort_order=EXCLUDED.sort_order, theme_config=EXCLUDED.theme_config,
                              translations=EXCLUDED.translations, structure=EXCLUDED.structure, updated_at=now();
