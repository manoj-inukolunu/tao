CREATE TABLE ${dbName}.`object` (
  `id` bigint NOT NULL,
  `time` bigint NOT NULL,
  `data` text,
  `type` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
