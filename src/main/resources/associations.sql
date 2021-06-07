CREATE TABLE ${dbName}.`associations` (
  `id1` bigint NOT NULL,
  `id2` bigint NOT NULL,
  `type` bigint NOT NULL,
  `timestamp` bigint NOT NULL,
  `data` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
