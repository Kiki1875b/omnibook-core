-- Property (숙소) 데이터 백필
INSERT INTO property (name, address) VALUES ('강남 럭셔리 호텔', '서울시 강남구 테헤란로 123'); -- ID: 1
INSERT INTO property (name, address) VALUES ('여기어때 시그니처 부산', '부산광역시 해운대구 우동 567-8'); -- ID: 2

-- Room (객실) 데이터 백필
-- 강남 럭셔리 호텔의 객실 (Property ID: 1)
INSERT INTO room (property_id, name, capacity) VALUES (1, '로열 스위트', 2); -- ID: 1
INSERT INTO room (property_id, name, capacity) VALUES (1, '디럭스 더블', 2); -- ID: 2
-- 여기어때 시그니처 부산의 객실 (Property ID: 2)
INSERT INTO room (property_id, name, capacity) VALUES (2, '디럭스 오션뷰', 2); -- ID: 3


-- ####################################################################
-- ## 객실 단위 매핑 (PlatformListing)
-- ## "하나의 객실이 여러 플랫폼에 각기 다른 ID로 등록된" 시나리오
-- ####################################################################
-- '로열 스위트'(Room ID:1)는 3개 플랫폼에 모두 다른 ID로 등록됨
INSERT INTO platform_listing (room_id, platform_type, platform_room_id) VALUES (1, 'AIRBNB', 'AIRBNB-TEST-ID-001'); -- Postman 테스트용 ID 1
INSERT INTO platform_listing (room_id, platform_type, platform_room_id) VALUES (1, 'YANOLJA', 'YANOLJA-TEST-ID-001');    -- Postman 테스트용 ID 2
INSERT INTO platform_listing (room_id, platform_type, platform_room_id) VALUES (1, 'YEOGIEOTTAE', 'YEO-TEST-ID-001');       -- Postman 테스트용 ID 3

-- '디럭스 더블'(Room ID:2)은 Airbnb에만 등록됨
INSERT INTO platform_listing (room_id, platform_type, platform_room_id) VALUES (2, 'AIRBNB', 'AIRBNB-TEST-ID-002');

-- '디럭스 오션뷰'(Room ID:3)는 Yanolja와 Yeogieottae에 등록됨
INSERT INTO platform_listing (room_id, platform_type, platform_room_id) VALUES (3, 'YANOLJA', 'YANOLJA-TEST-ID-002');
INSERT INTO platform_listing (room_id, platform_type, platform_room_id) VALUES (3, 'YEOGIEOTTAE', 'YEO-TEST-ID-002');
