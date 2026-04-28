-- Criar schema
CREATE SCHEMA IF NOT EXISTS `dbSmart40` DEFAULT CHARACTER SET utf8;
USE `dbSmart40`;

-- Tabela pedido (sem dependências)
CREATE TABLE IF NOT EXISTS `dbSmart40`.`pedido` (
  `id_pedido` INT NOT NULL AUTO_INCREMENT,
  `ordem_producao` VARCHAR(15) NOT NULL,
  `status` INT NOT NULL,
  `tipo` INT NOT NULL,
  `cor_tampa` INT NOT NULL,
  `criado_em` TIMESTAMP NOT NULL,
  `concluido_em` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id_pedido`),
  UNIQUE INDEX `ordem_producao_UNIQUE` (`ordem_producao` ASC) VISIBLE
);

-- Tabela estoque_posicao (inicialmente sem FK para bloco, para evitar ciclo)
CREATE TABLE IF NOT EXISTS `dbSmart40`.`estoque_posicao` (
  `id_estoque` INT NOT NULL AUTO_INCREMENT,
  `bloco_id_bloco` INT NOT NULL,
  PRIMARY KEY (`id_estoque`)
);

-- Tabela bloco (depende de pedido via ordem_producao e de estoque_posicao via id_estoque)
CREATE TABLE IF NOT EXISTS `dbSmart40`.`bloco` (
  `id_bloco` INT NOT NULL AUTO_INCREMENT,
  `ordem_producao` VARCHAR(15) NOT NULL,
  `cor_bloco` INT NOT NULL,
  `posicao_estoque_origem` INT NOT NULL,
  `criado_em` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id_bloco`),
  INDEX `fk_bloco_pedido_idx` (`ordem_producao` ASC) VISIBLE,
  INDEX `fk_bloco_estoque_idx` (`posicao_estoque_origem` ASC) VISIBLE,
  CONSTRAINT `fk_bloco_pedido_ordem`
    FOREIGN KEY (`ordem_producao`)
    REFERENCES `dbSmart40`.`pedido` (`ordem_producao`)
    ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_bloco_estoque_posicao`
    FOREIGN KEY (`posicao_estoque_origem`)
    REFERENCES `dbSmart40`.`estoque_posicao` (`id_estoque`)
    ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- Agora adicionar a FK em estoque_posicao para bloco (resolvendo o ciclo)
ALTER TABLE `dbSmart40`.`estoque_posicao`
  ADD INDEX `fk_estoque_posicao_bloco1_idx` (`bloco_id_bloco` ASC) VISIBLE,
  ADD CONSTRAINT `fk_estoque_posicao_bloco1`
    FOREIGN KEY (`bloco_id_bloco`)
    REFERENCES `dbSmart40`.`bloco` (`id_bloco`)
    ON DELETE NO ACTION ON UPDATE NO ACTION;

-- Tabela expedicao (depende de pedido via ordem_producao)
CREATE TABLE IF NOT EXISTS `dbSmart40`.`expedicao` (
  `id_expedicao` INT NOT NULL AUTO_INCREMENT,
  `ordem_producao` VARCHAR(15) NOT NULL,
  `posicao_expedicao` INT NOT NULL,
  `entrada_em` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id_expedicao`),
  INDEX `fk_expedicao_pedido_idx` (`ordem_producao` ASC) VISIBLE,
  CONSTRAINT `fk_expedicao_pedido_ordem`
    FOREIGN KEY (`ordem_producao`)
    REFERENCES `dbSmart40`.`pedido` (`ordem_producao`)
    ON DELETE NO ACTION ON UPDATE NO ACTION
);

-- Tabela lamina (depende de bloco)
CREATE TABLE IF NOT EXISTS `dbSmart40`.`lamina` (
  `id_lamina` INT NOT NULL AUTO_INCREMENT,
  `bloco_id_bloco` INT NOT NULL,
  `cor` INT NOT NULL,
  `padrao` INT NOT NULL,
  `posicao_no_bloco` INT NOT NULL,
  `criado_em` TIMESTAMP NOT NULL,
  PRIMARY KEY (`id_lamina`),
  INDEX `fk_lamina_bloco1_idx` (`bloco_id_bloco` ASC) VISIBLE,
  CONSTRAINT `fk_lamina_bloco1`
    FOREIGN KEY (`bloco_id_bloco`)
    REFERENCES `dbSmart40`.`bloco` (`id_bloco`)
    ON DELETE NO ACTION ON UPDATE NO ACTION
);