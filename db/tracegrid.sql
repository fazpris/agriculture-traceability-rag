/*
 Navicat Premium Data Transfer

 Source Server         : 毕业设计-Springboot
 Source Server Type    : MySQL
 Source Server Version : 80043
 Source Host           : localhost:3306
 Source Schema         : tracegrid

 Target Server Type    : MySQL
 Target Server Version : 80043
 File Encoding         : 65001

 Date: 21/04/2026 01:25:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for complaint_feedback
-- ----------------------------
DROP TABLE IF EXISTS `complaint_feedback`;
CREATE TABLE `complaint_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `trace_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '相关追溯码',
  `contact` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系方式',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '投诉内容',
  `status` tinyint NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已处理',
  `handle_result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '处理结果',
  `handle_by` bigint NULL DEFAULT NULL,
  `deleted` tinyint NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `handled_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '投诉反馈表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of complaint_feedback
-- ----------------------------
INSERT INTO `complaint_feedback` VALUES (1, 'AG-20260511-P001-100001-0A', '13900001001', '消费者咨询：平凉花牛苹果批次是否经过农残快检公示？', 0, NULL, NULL, 0, '2026-05-10 09:00:00', NULL);
INSERT INTO `complaint_feedback` VALUES (2, 'AG-20260511-P001-100002-1B', '13900001002', '建议补充定西马铃薯粉条加工环节的原料产地证明链接。', 0, NULL, NULL, 0, '2026-05-10 10:20:00', NULL);
INSERT INTO `complaint_feedback` VALUES (3, 'AG-20260511-P001-100003-2C', '13900001003', '投诉：民勤红枣包装日期与流通入库日期不一致，请核查。', 0, NULL, NULL, 0, '2026-05-10 14:05:00', NULL);

-- ----------------------------
-- Table structure for dist_inout_record
-- ----------------------------
DROP TABLE IF EXISTS `dist_inout_record`;
CREATE TABLE `dist_inout_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `trace_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '追溯码',
  `proc_batch_id` bigint NULL DEFAULT NULL,
  `source_proc_inventory_id` bigint NULL DEFAULT NULL COMMENT '关联加工成品出库流水id',
  `inout_type` tinyint NULL DEFAULT NULL COMMENT '1入库 2出库',
  `quantity` decimal(10, 2) NULL DEFAULT NULL,
  `operate_date` date NULL DEFAULT NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` tinyint NULL DEFAULT 0,
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '流通出入库记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dist_inout_record
-- ----------------------------

-- ----------------------------
-- Table structure for dist_logistics_node
-- ----------------------------
DROP TABLE IF EXISTS `dist_logistics_node`;
CREATE TABLE `dist_logistics_node`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `logistics_id` bigint NOT NULL,
  `node_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '节点名称',
  `location` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '位置',
  `arrive_time` datetime NULL DEFAULT NULL COMMENT '到达时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '物流节点' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dist_logistics_node
-- ----------------------------

-- ----------------------------
-- Table structure for dist_logistics_record
-- ----------------------------
DROP TABLE IF EXISTS `dist_logistics_record`;
CREATE TABLE `dist_logistics_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `trace_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `carrier` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '承运方',
  `waybill_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '运单号',
  `origin` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '起运地',
  `destination` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目的地',
  `status` tinyint NULL DEFAULT 0,
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '物流主记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dist_logistics_record
-- ----------------------------

-- ----------------------------
-- Table structure for dist_sale_record
-- ----------------------------
DROP TABLE IF EXISTS `dist_sale_record`;
CREATE TABLE `dist_sale_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `trace_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `store_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '门店名称',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '销售地址',
  `sale_date` datetime NULL DEFAULT NULL COMMENT '销售时间',
  `sale_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '销售价格',
  `purchase_method` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '购买方式',
  `anti_counterfeit_tip` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '防伪提示',
  `quantity` decimal(10, 2) NULL DEFAULT NULL,
  `unit` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` tinyint NULL DEFAULT 0,
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '终端销售记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of dist_sale_record
-- ----------------------------

-- ----------------------------
-- Table structure for proc_inventory_record
-- ----------------------------
DROP TABLE IF EXISTS `proc_inventory_record`;
CREATE TABLE `proc_inventory_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `product_batch_id` bigint NOT NULL,
  `operate_type` tinyint NULL DEFAULT NULL COMMENT '1入库 2出库 3盘点',
  `quantity` decimal(10, 2) NULL DEFAULT NULL,
  `operate_date` date NULL DEFAULT NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `linked_dist_inout_id` bigint NULL DEFAULT NULL COMMENT '核销对应的流通入库单ID',
  `dist_link_pending` tinyint NOT NULL DEFAULT 0 COMMENT '1待流通入库核销',
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成品库存记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of proc_inventory_record
-- ----------------------------

-- ----------------------------
-- Table structure for proc_process_record
-- ----------------------------
DROP TABLE IF EXISTS `proc_process_record`;
CREATE TABLE `proc_process_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `raw_inbound_id` bigint NOT NULL COMMENT '关联入库单',
  `process_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '加工类型（清洗/分拣/包装等）',
  `process_date` date NULL DEFAULT NULL,
  `output_quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '产出数量',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` tinyint NULL DEFAULT 0,
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '加工记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of proc_process_record
-- ----------------------------

-- ----------------------------
-- Table structure for proc_product_batch
-- ----------------------------
DROP TABLE IF EXISTS `proc_product_batch`;
CREATE TABLE `proc_product_batch`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `org_id` bigint NOT NULL,
  `raw_inbound_id` bigint NULL DEFAULT NULL,
  `product_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `product_spec` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '产品规格',
  `ingredients` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配料成分',
  `product_origin` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '产品产地',
  `shelf_life` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '保质期限',
  `storage_condition` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '储存条件',
  `finish_date` date NULL DEFAULT NULL COMMENT '完成日期',
  `quantity` decimal(10, 2) NULL DEFAULT NULL,
  `unit` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `quality_status` tinyint NULL DEFAULT 0 COMMENT '0待质检 1合格 2不合格',
  `status` tinyint NULL DEFAULT 0 COMMENT '0草稿 1已提交 2已生效',
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `batch_no`(`batch_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '成品批次' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of proc_product_batch
-- ----------------------------

-- ----------------------------
-- Table structure for proc_quality_inspection
-- ----------------------------
DROP TABLE IF EXISTS `proc_quality_inspection`;
CREATE TABLE `proc_quality_inspection`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `proc_batch_id` bigint NOT NULL COMMENT '关联成品批次',
  `inspect_date` date NULL DEFAULT NULL,
  `institution` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '检测机构',
  `result` tinyint NULL DEFAULT NULL COMMENT '1合格 2不合格',
  `conclusion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '结论说明',
  `report_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '检测报告附件',
  `images` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '质检报告相关的图片，JSON格式存储',
  `status` tinyint NULL DEFAULT 0 COMMENT '0待录入 1待审核 2合格 3不合格',
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '质检记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of proc_quality_inspection
-- ----------------------------

-- ----------------------------
-- Table structure for proc_raw_inbound
-- ----------------------------
DROP TABLE IF EXISTS `proc_raw_inbound`;
CREATE TABLE `proc_raw_inbound`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `prod_batch_id` bigint NOT NULL COMMENT '关联生产批次',
  `inbound_date` date NULL DEFAULT NULL COMMENT '入库日期',
  `quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '入库数量',
  `inspector` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '验收人',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` tinyint NULL DEFAULT 0 COMMENT '0草稿 1待审核 2审核通过 3审核驳回',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `audit_by` bigint NULL DEFAULT NULL COMMENT '审核人用户ID',
  `audit_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '原料入库' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of proc_raw_inbound
-- ----------------------------
-- ----------------------------
-- Table structure for prod_batch
-- ----------------------------
DROP TABLE IF EXISTS `prod_batch`;
CREATE TABLE `prod_batch`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '批次号',
  `org_id` bigint NOT NULL,
  `plot_id` bigint NULL DEFAULT NULL COMMENT '关联地块',
  `product_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '产品名称',
  `harvest_date` date NULL DEFAULT NULL COMMENT '采收日期',
  `quantity` decimal(10, 2) NULL DEFAULT NULL COMMENT '数量',
  `unit` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '单位（kg/个/箱）',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` tinyint NULL DEFAULT 0 COMMENT '0待审核 1审核通过 2审核驳回',
  `reject_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核驳回原因',
  `audit_by` bigint NULL DEFAULT NULL COMMENT '审核人用户ID',
  `audit_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `batch_no`(`batch_no` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '生产批次' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of prod_batch
-- ----------------------------

-- ----------------------------
-- Table structure for prod_farming_record
-- ----------------------------
DROP TABLE IF EXISTS `prod_farming_record`;
CREATE TABLE `prod_farming_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `plot_id` bigint NOT NULL COMMENT '关联地块',
  `action_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '播种/施肥/灌溉/用药/除草/采收',
  `action_date` date NOT NULL COMMENT '作业日期',
  `detail` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '详情描述',
  `operator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作人员',
  `status` tinyint NULL DEFAULT 0 COMMENT '0草稿 1已提交 2已生效 3已作废',
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `images` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '农事图片列表(JSON数组)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '农事记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of prod_farming_record
-- ----------------------------

-- ----------------------------
-- Table structure for prod_plot
-- ----------------------------
DROP TABLE IF EXISTS `prod_plot`;
CREATE TABLE `prod_plot`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `org_id` bigint NOT NULL,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '地块名称',
  `area` decimal(10, 2) NULL DEFAULT NULL COMMENT '面积（亩）',
  `location` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地理位置',
  `soil_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '土壤类型',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` tinyint NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '地块档案' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of prod_plot
-- ----------------------------
INSERT INTO `prod_plot` VALUES (1, 1, '静宁花牛苹果示范园A区', 35.00, '甘肃省平凉市静宁县', '黄土高原褐土', '陇东苹果优势带', 1, 0, 2, '2026-04-01 09:00:00', NULL, NULL);
INSERT INTO `prod_plot` VALUES (2, 1, '定西马铃薯旱作示范田', 80.00, '甘肃省定西市安定区', '黄绵土', '全膜双垄沟播', 1, 0, 2, '2026-04-01 09:05:00', NULL, NULL);
INSERT INTO `prod_plot` VALUES (3, 1, '民勤绿洲灰枣种植带', 42.00, '甘肃省武威市民勤县', '灌淤土', '膜下滴灌节水', 1, 0, 2, '2026-04-01 09:10:00', NULL, NULL);

INSERT INTO `prod_farming_record` VALUES (1, 1, 1, '灌溉', '2026-04-12', '果园滴灌补水，保障花牛苹果膨大期水分', '马建军', 2, 0, 2, '2026-04-12 08:00:00', NULL, NULL, NULL);
INSERT INTO `prod_farming_record` VALUES (2, 1, 2, '施肥', '2026-04-18', '马铃薯块茎形成期追施高钾复合肥', '李迎春', 2, 0, 2, '2026-04-18 10:00:00', NULL, NULL, NULL);
INSERT INTO `prod_farming_record` VALUES (3, 1, 3, '采收', '2026-05-05', '灰枣自然吊干达标后分批采收，糖度抽检合格', '张雪莲', 2, 0, 2, '2026-05-05 07:30:00', NULL, NULL, NULL);

INSERT INTO `prod_batch` VALUES (1, 'PB-NW-20260511-0001', 1, 1, '平凉花牛苹果（一级果）', '2026-05-01', 5200.00, 'kg', '静宁产区头茬采收', 1, NULL, 1, '2026-05-02 10:00:00', 0, 2, '2026-05-01 18:00:00', NULL, NULL);
INSERT INTO `prod_batch` VALUES (2, 'PB-NW-20260511-0002', 1, 2, '定西马铃薯', '2026-05-03', 12000.00, 'kg', '旱作区地膜覆盖种植', 1, NULL, 1, '2026-05-04 09:00:00', 0, 2, '2026-05-03 17:30:00', NULL, NULL);
INSERT INTO `prod_batch` VALUES (3, 'PB-NW-20260511-0003', 1, 3, '民勤灰枣（吊干原枣）', '2026-05-05', 3000.00, 'kg', '沙漠绿洲灌溉农业', 1, NULL, 1, '2026-05-05 16:00:00', 0, 2, '2026-05-05 15:00:00', NULL, NULL);

INSERT INTO `proc_raw_inbound` VALUES (1, 2, 1, '2026-05-06', 5000.00, '王检验', '陇东花牛苹果原料果验收', 2, NULL, 1, '2026-05-06 16:00:00', 0, 3, '2026-05-06 15:00:00', NULL, NULL);
INSERT INTO `proc_raw_inbound` VALUES (2, 2, 2, '2026-05-07', 8000.00, '王检验', '定西马铃薯全粉原料入库', 2, NULL, 1, '2026-05-07 11:00:00', 0, 3, '2026-05-07 10:30:00', NULL, NULL);
INSERT INTO `proc_raw_inbound` VALUES (3, 2, 3, '2026-05-08', 3000.00, '王检验', '民勤灰枣干果原料入库', 2, NULL, 1, '2026-05-08 09:00:00', 0, 3, '2026-05-08 08:30:00', NULL, NULL);

INSERT INTO `proc_product_batch` VALUES (1, 'CB-NW-20260511-0001', 2, 1, '平凉花牛苹果脆片', '150g/袋', '花牛苹果、麦芽糖浆', '甘肃省平凉市', '12个月', '阴凉干燥', '2026-05-07', 800.00, '袋', 1, 2, 0, 3, '2026-05-07 10:00:00', NULL, NULL);
INSERT INTO `proc_product_batch` VALUES (2, 'CB-NW-20260511-0002', 2, 2, '定西马铃薯粉条', '2.5kg/把', '马铃薯淀粉、饮用水', '甘肃省定西市', '18个月', '避光防潮', '2026-05-08', 1200.00, '把', 1, 2, 0, 3, '2026-05-08 14:00:00', NULL, NULL);
INSERT INTO `proc_product_batch` VALUES (3, 'CB-NW-20260511-0003', 2, 3, '民勤灰枣夹核桃仁', '500g/盒', '灰枣、核桃仁', '甘肃省武威市民勤县', '10个月', '冷藏更佳', '2026-05-09', 500.00, '盒', 1, 2, 0, 3, '2026-05-09 09:00:00', NULL, NULL);

-- ----------------------------
-- Table structure for sys_message
-- ----------------------------
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '接收者ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '消息内容',
  `type` int NULL DEFAULT 0 COMMENT '0消息 1审核 2投诉',
  `status` int NULL DEFAULT 0 COMMENT '0未读 1已读',
  `deleted` int NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_message
-- ----------------------------

-- ----------------------------
-- Table structure for sys_org
-- ----------------------------
DROP TABLE IF EXISTS `sys_org`;
CREATE TABLE `sys_org`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主体名称',
  `type` tinyint NOT NULL COMMENT '1生产 2加工 3流通',
  `address` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '地址',
  `contact` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系人',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '联系电话',
  `license_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '营业执照号',
  `proc_dist_link_outbound` tinyint NOT NULL DEFAULT 0 COMMENT '加工主体：成品出库是否产生待核销',
  `proc_dist_link_inbound` tinyint NOT NULL DEFAULT 0 COMMENT '流通主体：入库须关联加工出库',
  `status` tinyint NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `deleted` tinyint NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '组织/主体表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_org
-- ----------------------------
INSERT INTO `sys_org` VALUES (1, '甘肃丝路生态农业合作社', 1, '甘肃省平凉市静宁县威戎镇', '马建军', '0933-0000001', '91620000MA7NW00001', 0, 0, 1, 0, '2026-05-01 08:00:00', NULL);
INSERT INTO `sys_org` VALUES (2, '陇原果品精深加工有限公司', 2, '甘肃省兰州市榆中县和平工业园', '王检验', '0931-0000002', '91620000MA7NW00002', 1, 0, 1, 0, '2026-05-01 08:00:00', NULL);
INSERT INTO `sys_org` VALUES (3, '西北鲜链冷链物流有限公司', 3, '新疆维吾尔自治区乌鲁木齐市头屯河区', '刘调度', '0991-0000003', '91650000MA7NW00003', 0, 1, 1, 0, '2026-05-01 08:00:00', NULL);

-- ----------------------------
-- Table structure for sys_register_apply
-- ----------------------------
DROP TABLE IF EXISTS `sys_register_apply`;
CREATE TABLE `sys_register_apply`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '加密密码',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `role` tinyint NOT NULL COMMENT '2生产 3加工 4流通',
  `org_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主体名称',
  `org_type` tinyint NOT NULL,
  `org_address` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `license_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `apply_kind` tinyint NOT NULL DEFAULT 0 COMMENT '0入驻 1认证变更',
  `target_org_id` bigint NULL DEFAULT NULL COMMENT '认证变更时关联主体',
  `status` tinyint NULL DEFAULT 0 COMMENT '0待审核 1通过 2驳回',
  `reject_reason` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `audit_by` bigint NULL DEFAULT NULL,
  `audit_at` datetime NULL DEFAULT NULL,
  `deleted` tinyint NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '注册申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_register_apply
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BCrypt加密密码',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `role` tinyint NOT NULL COMMENT '1管理员 2生产 3加工 4流通',
  `org_id` bigint NULL DEFAULT NULL COMMENT '所属主体ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `deleted` tinyint NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$u5j1CK7b/epP85VFDiinIONukPt3gMfruzTWBDFcx3WqnBULPyjOq', '平台管理员', '13800000001', 1, NULL, 1, 0, '2026-05-01 08:00:00', NULL);
INSERT INTO `sys_user` VALUES (2, 'sc', '$2a$10$u5j1CK7b/epP85VFDiinIONukPt3gMfruzTWBDFcx3WqnBULPyjOq', '生产管理员', '13800000002', 2, 1, 1, 0, '2026-05-01 08:00:00', NULL);
INSERT INTO `sys_user` VALUES (3, 'jg', '$2a$10$u5j1CK7b/epP85VFDiinIONukPt3gMfruzTWBDFcx3WqnBULPyjOq', '加工管理员', '13800000003', 3, 2, 1, 0, '2026-05-01 08:00:00', NULL);
INSERT INTO `sys_user` VALUES (4, 'lt', '$2a$10$u5j1CK7b/epP85VFDiinIONukPt3gMfruzTWBDFcx3WqnBULPyjOq', '流通管理员', '13800000004', 4, 3, 1, 0, '2026-05-01 08:00:00', NULL);

-- ----------------------------
-- Table structure for trace_code
-- ----------------------------
DROP TABLE IF EXISTS `trace_code`;
CREATE TABLE `trace_code`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '追溯码如AG-20260325-P001-000123-X8',
  `prod_batch_id` bigint NULL DEFAULT NULL COMMENT '关联生产批次',
  `proc_batch_id` bigint NULL DEFAULT NULL COMMENT '关联成品批次',
  `qr_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '二维码图片路径',
  `status` tinyint NULL DEFAULT 1 COMMENT '0未启用 1已生成 2已打印 3已入库 4已流通 5已售出 6已作废',
  `deleted` tinyint NULL DEFAULT 0,
  `created_by` bigint NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint NULL DEFAULT NULL,
  `updated_at` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `scan_count` int NULL DEFAULT 0 COMMENT '扫码查验次数',
  `first_scan_time` datetime NULL DEFAULT NULL COMMENT '首次查验时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '追溯码' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of trace_code
-- ----------------------------
INSERT INTO `trace_code` VALUES (1, 'AG-20260511-P001-100001-0A', 1, 1, NULL, 4, 0, 3, '2026-05-07 12:00:00', NULL, NULL, 0, NULL);
INSERT INTO `trace_code` VALUES (2, 'AG-20260511-P001-100002-1B', 2, 2, NULL, 4, 0, 3, '2026-05-08 14:30:00', NULL, NULL, 0, NULL);
INSERT INTO `trace_code` VALUES (3, 'AG-20260511-P001-100003-2C', 3, 3, NULL, 4, 0, 3, '2026-05-09 10:00:00', NULL, NULL, 0, NULL);

INSERT INTO `dist_inout_record` VALUES (1, 3, 'AG-20260511-P001-100001-0A', NULL, NULL, 1, 800.00, '2026-05-08', '乌鲁木齐冷链中心入库：平凉花牛苹果（礼盒）', 0, 0, 4, '2026-05-08 11:00:00', NULL, NULL);
INSERT INTO `dist_inout_record` VALUES (2, 3, 'AG-20260511-P001-100002-1B', NULL, NULL, 1, 1200.00, '2026-05-09', '入库：定西马铃薯粉条（箱装）', 0, 0, 4, '2026-05-09 09:30:00', NULL, NULL);
INSERT INTO `dist_inout_record` VALUES (3, 3, 'AG-20260511-P001-100003-2C', NULL, NULL, 2, 500.00, '2026-05-10', '出库调拨：民勤灰枣至河西终端仓', 0, 0, 4, '2026-05-10 15:00:00', NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
