# DirectRadio — предварительный сетевой протокол

Каждый пакет содержит: protocol_version, network_id, sender_id, destination/channel_id, packet_id, sequence, hop_limit, message_type, timestamp/nonce, payload_length, encrypted_payload, authentication_tag.

Типы сообщений: HELLO, PEER_LIST, ROUTE_UPDATE, CHANNEL_JOIN, CHANNEL_LEAVE, FLOOR_REQUEST, FLOOR_GRANT, FLOOR_RELEASE, AUDIO_FRAME, PING, PONG, DISCONNECT.

Правила Mesh:
- Каждый узел хранит соседей и ограниченную таблицу маршрутов.
- packet_id дедуплицируется в ограниченном временном окне.
- hop_limit уменьшается при каждом ретрансляционном переходе; пакет с нулевым TTL отбрасывается.
- Не ретранслировать пакет обратно на входящий интерфейс.
- Голосовые кадры имеют короткий срок годности; просроченные кадры отбрасываются.
- Управляющие сообщения и голос имеют отдельные очереди; голосу назначается приоритет.

Это предварительная схема, не финальная спецификация совместимости.