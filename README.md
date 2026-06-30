# Smart 4.0 — Backend (API)

Backend em Spring Boot responsável por orquestrar o ciclo de produção da bancada Smart 4.0:
recebimento de pedidos, comunicação com os CLPs (S7) das estações de **Estoque**, **Processo**,
**Montagem** e **Expedição**, e exposição de status em tempo real para o frontend.

---

## 1. Pré-requisitos

| Ferramenta | Versão recomendada |
|---|---|
| JDK | 17+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| IDE (opcional) | VS Code / IntelliJ / Eclipse com suporte a Spring Boot |

Certifique-se também de ter acesso de rede aos CLPs (protocolo S7, porta `102`) caso queira
testar a comunicação real com as bancadas. Sem essa conexão, os endpoints de leitura/escrita
no CLP retornarão erro de conexão, mas o restante da API (cadastro de pedidos, estoque, etc.)
funciona normalmente.

---

## 2. Configuração do banco de dados (MySQL)

1. Crie o banco de dados:

   ```sql
   CREATE DATABASE smart40 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. Execute o script SQL inicial (schema + dados de seed) fornecido junto ao projeto contra
   esse banco. Exemplo via linha de comando:

   ```bash
   mysql -u <usuario> -p smart40 < script_inicial.sql
   ```

   Ou, se preferir, importe o arquivo pela sua ferramenta de administração (MySQL Workbench,
   DBeaver, etc.), apontando para o banco `smart40` criado no passo anterior.

3. Confirme que as tabelas principais foram criadas: `pedidos`, `bloco`, `estoque`, `lamina`,
   `expedicao` (e demais tabelas de apoio definidas no script).

> O projeto usa Spring Data JPA. Se preferir não rodar o script manualmente, é possível deixar
> o Hibernate criar o schema automaticamente configurando
> `spring.jpa.hibernate.ddl-auto=update` — porém isso **não** popula dados iniciais (cores de
> estoque, posições, etc.), então o uso do script SQL é a forma recomendada para este projeto.

---

## 3. Variáveis de ambiente / configuração

As configurações ficam em `src/main/resources/application.properties` (ou
`application.yml`). As principais propriedades a ajustar antes de subir o projeto são:

```properties
# --- Servidor ---
server.port=8080

# --- Banco de dados (MySQL) ---
spring.datasource.url=jdbc:mysql://localhost:3306/smart40?useSSL=false&serverTimezone=UTC
spring.datasource.username=<usuario>
spring.datasource.password=<senha>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# --- JPA / Hibernate ---
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# --- Integração com APIs externas (estoque/expedição) ---
api.estoque.url=http://<host>:<porta>/api-externa-estoque
api.expedicao.url=http://<host>:<porta>/api-externa-expedicao
api.seletor-tampas-present=false

# --- CLPs (opcional, dependendo de como ApiUrlConfig está implementado) ---
# Os IPs dos CLPs também podem ser enviados diretamente no corpo da requisição
# POST /start-readings, então não é estritamente necessário fixá-los aqui.
```

Ajuste os valores acima conforme o ambiente onde o projeto será executado. Se o seletor de
tampas (ESP32) não estiver fisicamente presente na bancada, mantenha
`api.seletor-tampas-present=false` para evitar tentativas de chamada a um dispositivo
inexistente.

> ⚠️ Não versione credenciais reais de banco de dados. Para ambientes de demonstração, use um
> usuário MySQL dedicado com permissões restritas ao schema `smart40`.

---

## 4. Build e execução

### Via Maven Wrapper

```bash
# Linux / macOS
./mvnw clean install
./mvnw spring-boot:run

# Windows
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

### Via JAR empacotado

```bash
./mvnw clean package -DskipTests
java -jar target/<nome-do-artefato>.jar
```

A API sobe, por padrão, em `http://localhost:8080`.

---

## 5. Principais grupos de endpoints

| Grupo | Base path | Responsabilidade |
|---|---|---|
| Pedidos | `/pedidos` | CRUD de pedidos, geração de config/info, envio ao CLP (`/iniciar`) |
| Estoque | `/api/estoque` | CRUD de posições de estoque |
| Blocos | `/api/bloco` | CRUD de blocos e vínculo com estoque/pedido |
| Lâminas | `/api/lamina` | CRUD de lâminas vinculadas a blocos |
| Expedição | `/api/expedicao` | CRUD de registros de expedição |
| Produção | `/producao/{id}/gravar`, `/producao/{id}/iniciar` | Grava o pedido no CLP e inicia a produção |
| CLP / Leituras | `/start-readings`, `/stop-readings`, `/status`, `/smartstream/{bancada}` | Controle das leituras cíclicas dos CLPs e status agregado das estações |
| Utilitários | `/smart/ping`, `/smart/reset-status`, `/smart/readonly` | Teste de conectividade com CLPs, reset de status e modo somente-leitura |

A documentação completa e testável de cada endpoint (parâmetros, corpo de requisição,
respostas) está disponível via Swagger — veja a seção seguinte.

---

## 6. Documentação da API (Swagger / OpenAPI)

Com a aplicação em execução, acesse:

```
http://localhost:8080/swagger-ui/index.html
```

(ou `http://localhost:8080/swagger-ui.html`, dependendo da versão do springdoc/springfox usada
no `pom.xml`).

O JSON da especificação OpenAPI fica disponível em:

```
http://localhost:8080/v3/api-docs
```

> Caso o projeto ainda não tenha a dependência do Swagger/OpenAPI configurada, adicione ao
> `pom.xml`:
> ```xml
> <dependency>
>     <groupId>org.springdoc</groupId>
>     <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
>     <version>2.5.0</version>
> </dependency>
> ```
> Não é necessária nenhuma configuração adicional para os endpoints `@RestController`
> existentes serem documentados automaticamente.

---

## 7. Roteiro de demonstração (ciclo completo)

Sugestão de sequência para a demo funcional do ciclo
**entrada no estoque → pedido → produção → expedição**:

1. **Entrada no estoque**
   - `POST /api/estoque/salvar` — cadastra um bloco físico em uma posição do estoque
     (`posicaoEstoque`, `cor`).

2. **Criação do pedido**
   - `POST /pedidos` — cria um pedido informando tipo, cor da tampa e blocos desejados.
     O sistema reserva automaticamente as posições de estoque correspondentes às cores
     pedidas.

3. **Conferência dos dados antes do envio**
   - `GET /pedidos/{id}/config` — visualiza o DTO de configuração (blocos, cores, IP do CLP).
   - `GET /pedidos/{id}/info` — visualiza o DTO com os dados detalhados por andar, prontos
     para envio ao CLP.

4. **Início das leituras dos CLPs**
   - `POST /start-readings` com o corpo:
     ```json
     { "estoque": "10.74.241.10", "processo": "10.74.241.20", "montagem": "10.74.241.30", "expedicao": "10.74.241.40" }
     ```

5. **Envio do pedido para produção**
   - `POST /pedidos/{id}/iniciar` (ou `POST /producao/{id}/gravar` seguido de
     `POST /producao/{id}/iniciar`) — grava os dados no CLP e dispara o início da execução.

6. **Acompanhamento em tempo real**
   - `GET /status` — consulta pontual do status agregado de todas as estações.
   - `GET /smartstream/{bancada}` (SSE) — acompanhamento contínuo de uma estação específica
     (`estoque`, `processo`, `montagem` ou `expedicao`).

7. **Conclusão**
   - Quando a expedição confirma a retirada do pedido, `PATCH /pedidos/{id}/concluir` marca o
     pedido como concluído e gera o registro de expedição.

8. **Encerramento das leituras**
   - `POST /stop-readings` — interrompe o polling cíclico e fecha as conexões com os CLPs.

---

## 8. Estrutura geral do projeto

```
src/main/java/com/sa/smart/
├── controller/   # Endpoints REST (Pedido, Estoque, Bloco, Lamina, Expedicao, CLP, Producao)
├── service/      # Regras de negócio e comunicação com os CLPs (SmartService e específicos por estação)
├── model/        # Entidades JPA
├── repository/   # Interfaces Spring Data JPA
├── dto/          # DTOs de entrada/saída (records e classes @Data)
├── enums/        # Enums de domínio (cor do bloco, status, tipo de pedido)
├── config/       # Configurações (URLs de APIs externas, seletor de tampas, etc.)
└── clpcomm/      # Camada de comunicação S7 com os CLPs (PlcConnector, PlcReaderDB, etc.)
```

---

## 9. Solução de problemas comuns

| Sintoma | Possível causa |
|---|---|
| `Erro ao obter conexão com o CLP` | IP incorreto, CLP desligado, ou rede/firewall bloqueando a porta 102 |
| `/status` retorna campos zerados | Nenhuma leitura foi iniciada ainda (`POST /start-readings` não foi chamado) |
| Erro de conexão com o banco ao subir a aplicação | Verifique `spring.datasource.url`/usuário/senha e se o MySQL está no ar |
| `Nenhuma posição de estoque disponível com a cor: X` | Não há blocos cadastrados em `/api/estoque` com a cor solicitada |
| Seletor de tampas retorna erro | Verifique `api.seletor-tampas-present` — desative se o ESP32 não estiver presente na bancada |
