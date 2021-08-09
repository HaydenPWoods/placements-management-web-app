-- MySQL dump 10.13  Distrib 8.0.22, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: hpw7
-- ------------------------------------------------------
-- Server version	8.0.22
-- ------------------------------------------------------
-- hpw7_no_data.sql
-- ------------------------------------------------------
-- This dump creates the table structure *without* any
-- existing example data.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `app_user`
--

DROP TABLE IF EXISTS `app_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_user` (
  `username` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(255) NOT NULL,
  `members_id` int DEFAULT NULL,
  `device_token` varchar(255) DEFAULT NULL,
  `emails_enabled` bit(1) NOT NULL,
  `profile_picture` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`username`),
  KEY `FK2cpmx0chrpmn4v0847g8fligm` (`members_id`),
  CONSTRAINT `FK2cpmx0chrpmn4v0847g8fligm` FOREIGN KEY (`members_id`) REFERENCES `provider` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `authorisation_request`
--

DROP TABLE IF EXISTS `authorisation_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `authorisation_request` (
  `id` int NOT NULL AUTO_INCREMENT,
  `approved_by_administrator` bit(1) NOT NULL,
  `approved_by_tutor` bit(1) NOT NULL,
  `details` longtext NOT NULL,
  `end_date` date NOT NULL,
  `provider_address_city` varchar(255) NOT NULL,
  `provider_address_line1` varchar(255) NOT NULL,
  `provider_address_line2` varchar(255) DEFAULT NULL,
  `provider_address_postcode` varchar(255) NOT NULL,
  `provider_name` varchar(255) NOT NULL,
  `start_date` date NOT NULL,
  `title` varchar(255) NOT NULL,
  `tutor_username` varchar(255) NOT NULL,
  `app_user_username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9v63ko0fs2ca48xjjwm4nu5u` (`tutor_username`),
  KEY `FKclo35fdsvymiokx26d302na10` (`app_user_username`),
  FULLTEXT KEY `full_text_search` (`title`,`provider_name`,`details`,`provider_address_line1`,`provider_address_line2`,`provider_address_city`,`provider_address_postcode`),
  CONSTRAINT `FK9v63ko0fs2ca48xjjwm4nu5u` FOREIGN KEY (`tutor_username`) REFERENCES `app_user` (`username`),
  CONSTRAINT `FKclo35fdsvymiokx26d302na10` FOREIGN KEY (`app_user_username`) REFERENCES `app_user` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `document`
--

DROP TABLE IF EXISTS `document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document` (
  `id` int NOT NULL AUTO_INCREMENT,
  `description` varchar(255) NOT NULL,
  `file` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `owner_username` varchar(255) NOT NULL,
  `placement_application_id` int DEFAULT NULL,
  `authorisation_request_id` int DEFAULT NULL,
  `placement_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhn8680lisxnbjuxbtg94fn0g7` (`owner_username`),
  KEY `FK7aloavrkfjh2q91bppgpfhv8q` (`placement_application_id`),
  KEY `FK4eib8awldlb72wm4rcsfg7kjb` (`placement_id`),
  KEY `FKgetp28gslu448cbffmh48mjbp` (`authorisation_request_id`),
  CONSTRAINT `FK4eib8awldlb72wm4rcsfg7kjb` FOREIGN KEY (`placement_id`) REFERENCES `placement` (`id`),
  CONSTRAINT `FK7aloavrkfjh2q91bppgpfhv8q` FOREIGN KEY (`placement_application_id`) REFERENCES `placement_application` (`id`),
  CONSTRAINT `FKgetp28gslu448cbffmh48mjbp` FOREIGN KEY (`authorisation_request_id`) REFERENCES `authorisation_request` (`id`),
  CONSTRAINT `FKhn8680lisxnbjuxbtg94fn0g7` FOREIGN KEY (`owner_username`) REFERENCES `app_user` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `duration_to_provider`
--

DROP TABLE IF EXISTS `duration_to_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `duration_to_provider` (
  `id` int NOT NULL AUTO_INCREMENT,
  `duration` bigint NOT NULL,
  `pr2_id` int NOT NULL,
  `pr1_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8xtvaolibwn6ndyrh6h5sd7yb` (`pr2_id`),
  KEY `FKi65hthdc4ogsul757n649we3a` (`pr1_id`),
  CONSTRAINT `FK8xtvaolibwn6ndyrh6h5sd7yb` FOREIGN KEY (`pr2_id`) REFERENCES `provider` (`id`),
  CONSTRAINT `FKi65hthdc4ogsul757n649we3a` FOREIGN KEY (`pr1_id`) REFERENCES `provider` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `log_entry`
--

DROP TABLE IF EXISTS `log_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_entry` (
  `id` int NOT NULL AUTO_INCREMENT,
  `action_type` varchar(255) NOT NULL,
  `description` varchar(255) NOT NULL,
  `entity_type` varchar(255) NOT NULL,
  `timestamp` timestamp NOT NULL,
  `app_user` varchar(255) NOT NULL,
  `entity_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKtphwp14gtwb4oc2ww27rbdla6` (`app_user`),
  CONSTRAINT `FKtphwp14gtwb4oc2ww27rbdla6` FOREIGN KEY (`app_user`) REFERENCES `app_user` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=595 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` int NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `timestamp` timestamp NOT NULL,
  `recipient` varchar(255) NOT NULL,
  `sender` varchar(255) NOT NULL,
  `salt` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK25h6sbo04yqupck7c72mg2h8h` (`recipient`),
  KEY `FKjnl1hp0atnmqgu6xk3moybws3` (`sender`),
  CONSTRAINT `FK25h6sbo04yqupck7c72mg2h8h` FOREIGN KEY (`recipient`) REFERENCES `app_user` (`username`),
  CONSTRAINT `FKjnl1hp0atnmqgu6xk3moybws3` FOREIGN KEY (`sender`) REFERENCES `app_user` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=326 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `placement`
--

DROP TABLE IF EXISTS `placement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `placement` (
  `id` int NOT NULL AUTO_INCREMENT,
  `application_deadline` datetime NOT NULL,
  `description` longtext NOT NULL,
  `end_date` date NOT NULL,
  `start_date` date NOT NULL,
  `title` varchar(255) NOT NULL,
  `provider_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgq6rardrimti4395uo71k54uw` (`provider_id`),
  FULLTEXT KEY `full_text_search` (`title`,`description`),
  CONSTRAINT `FKgq6rardrimti4395uo71k54uw` FOREIGN KEY (`provider_id`) REFERENCES `provider` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `placement_application`
--

DROP TABLE IF EXISTS `placement_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `placement_application` (
  `id` int NOT NULL AUTO_INCREMENT,
  `approved_by_administrator` bit(1) NOT NULL,
  `approved_by_provider` bit(1) NOT NULL,
  `details` longtext NOT NULL,
  `placement_id` int NOT NULL,
  `app_user_username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKe5jq4ev27e1tmcchfpfw0x5bp` (`placement_id`),
  KEY `FKagstlhrr3ud93trp5fafqvqlm` (`app_user_username`),
  CONSTRAINT `FKagstlhrr3ud93trp5fafqvqlm` FOREIGN KEY (`app_user_username`) REFERENCES `app_user` (`username`),
  CONSTRAINT `FKe5jq4ev27e1tmcchfpfw0x5bp` FOREIGN KEY (`placement_id`) REFERENCES `placement` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `placement_members`
--

DROP TABLE IF EXISTS `placement_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `placement_members` (
  `placement_id` int NOT NULL,
  `members_username` varchar(255) NOT NULL,
  KEY `FKsu26qb64esrt4rg1ada5noth7` (`members_username`),
  KEY `FK88plj5fdu0stf59xtgvu4t6kf` (`placement_id`),
  CONSTRAINT `FK88plj5fdu0stf59xtgvu4t6kf` FOREIGN KEY (`placement_id`) REFERENCES `placement` (`id`),
  CONSTRAINT `FKsu26qb64esrt4rg1ada5noth7` FOREIGN KEY (`members_username`) REFERENCES `app_user` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `placement_visit`
--

DROP TABLE IF EXISTS `placement_visit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `placement_visit` (
  `id` int NOT NULL AUTO_INCREMENT,
  `visit_date_time` datetime NOT NULL,
  `organiser_username` varchar(255) NOT NULL,
  `placement_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKeu2lw1g5nnykkwb22t8fm529e` (`organiser_username`),
  KEY `FKo9j2enb8i66cnxby0opoil9v3` (`placement_id`),
  CONSTRAINT `FKeu2lw1g5nnykkwb22t8fm529e` FOREIGN KEY (`organiser_username`) REFERENCES `app_user` (`username`),
  CONSTRAINT `FKo9j2enb8i66cnxby0opoil9v3` FOREIGN KEY (`placement_id`) REFERENCES `placement` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `provider`
--

DROP TABLE IF EXISTS `provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `provider` (
  `id` int NOT NULL AUTO_INCREMENT,
  `address_city` varchar(255) NOT NULL,
  `address_line1` varchar(255) NOT NULL,
  `address_line2` varchar(255) DEFAULT NULL,
  `address_postcode` varchar(255) NOT NULL,
  `description` longtext NOT NULL,
  `latitude` double NOT NULL,
  `logo` varchar(255) DEFAULT NULL,
  `longitude` double NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  FULLTEXT KEY `full_text_search` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'hpw7'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2021-05-02 10:26:01
