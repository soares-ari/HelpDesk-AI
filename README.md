# Helpdesk AI

Sistema RAG (Retrieval-Augmented Generation) enterprise para Q&A sobre documentação técnica em PDF.

## 📋 Visão Geral

Helpdesk AI permite que usuários façam upload de documentos PDF (APIs, manuais técnicos, documentação interna) e conversem com um chatbot inteligente que responde perguntas baseadas no conteúdo indexado, com citações precisas das fontes.

## 🏗️ Arquitetura

```
┌─────────────┐      ┌──────────────────┐      ┌─────────────┐
│   Angular   │────▶│  Spring Boot API │────▶│  PostgreSQL │
│   Frontend  │◀────│   (REST + JWT)   │◀────│  + pgvector │
└─────────────┘      └──────────────────┘      └─────────────┘
                              │
                              ▼
                     ┌──────────────┐
                     │  OpenAI API  │
                     │  - Embeddings│
                     │  - Chat GPT  │
                     └──────────────┘
```

## 🛠️ Stack Tecnológica

### Backend
- **Java 21** (toolchain Maven já configurada para Temurin 21)
- **Spring Boot 3.3.0** - Framework principal
- **Spring AI 1.0.0-M4** - Integração com LLMs
- **PostgreSQL 16 + pgvector 0.8.1** - Banco de dados vetorial
- **Apache Tika 2.9.1** - Extração de texto de PDFs
- **Spring Security + JWT** - Autenticação
- **Maven** - Gestão de dependências

### Frontend
- **Angular 17+**
- **TypeScript**
- **PrimeNG** - Componentes UI enterprise
- **RxJS** - Programação reativa

### AI/LLM
- **OpenAI API**
  - GPT-4 Turbo (chat)
  - text-embedding-3-small (embeddings)

### Infraestrutura
- **Docker Compose** - Desenvolvimento local
- **Railway** - Deploy backend
- **Vercel** - Deploy frontend

## 🚀 Quick Start

### Pré-requisitos

- Java 21 (Temurin recomendado)
- Maven 3.9+ ([Download Maven](https://maven.apache.org/download.cgi))
- Node.js 18+ ([Download Node](https://nodejs.org/))
- Docker Desktop ([Download Docker](https://www.docker.com/products/docker-desktop))
- Conta OpenAI com API Key ([OpenAI Platform](https://platform.openai.com/))

### 1. Clonar o Repositório

```bash
git clone <repository-url>
cd helpdesk-ai
```

### 2. Configurar Banco de Dados (PostgreSQL + pgvector)

```bash
cd docker
docker-compose up -d
```

Verificar instalação do pgvector:
```bash
docker exec helpdesk-ai-db psql -U postgres -d helpdesk_ai -c "\dx"
```

**Nota:** O banco está rodando na porta `5433` (não 5432) para evitar conflitos.

### 3. Configurar Backend

> Nota: o Maven usa o toolchain em `.mvn/toolchains.xml` apontando para `C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot`. Ajuste o caminho se instalou o JDK 21 em outro diretório.

Criar arquivo `backend/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/helpdesk_ai
    username: postgres
    password: postgres

  ai:
    openai:
      api-key: sk-your-openai-api-key-here

jwt:
  secret: your-256-bit-secret-key-change-in-production
```

Compilar e executar:
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

Swagger UI disponível em: `http://localhost:8080/api/swagger-ui.html`

### 4. Configurar Frontend

```bash
cd frontend
npm install
npm start
```

Aplicação disponível em: `http://localhost:4200`

## 📁 Estrutura do Projeto

```
helpdesk-ai/
├── backend/                    # Spring Boot API
│   ├── src/main/java/com/helpdeskai/
│   │   ├── config/            # Configurações (Security, OpenAI, pgvector)
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Lógica de negócio
│   │   ├── repository/        # JPA repositories
│   │   ├── entity/            # Entidades JPA
│   │   ├── dto/               # Request/Response objects
│   │   └── security/          # JWT providers, filters
│   └── pom.xml
│
├── frontend/                   # Angular SPA
│   ├── src/app/
│   │   ├── core/              # Services, guards, interceptors
│   │   ├── features/          # Módulos de features (auth, chat, documents)
│   │   └── shared/            # Componentes compartilhados
│   └── package.json
│
├── docker/                     # Docker Compose
│   ├── docker-compose.yml
│   └── init-db.sql
│
└── docs/                       # Documentação
```

## 🔑 Funcionalidades Core

### Backend

#### POST `/api/documents/upload`
Upload e indexação de PDF:
1. Extrai texto com Apache Tika
2. Chunking inteligente (600-800 tokens, overlap 150)
3. Gera embeddings (OpenAI)
4. Armazena no pgvector

#### POST `/api/chat`
Chat com RAG:
1. Gera embedding da pergunta
2. Busca top-5 chunks similares (cosine similarity)
3. Monta prompt com contexto
4. Chama GPT-4
5. Retorna resposta + citações

### Frontend

- Upload de documentos (drag & drop)
- Interface de chat corporativa
- Exibição de fontes citadas
- Histórico de conversas
- Dashboard de métricas

## 🧪 Testes

### Backend

O projeto possui uma suíte abrangente de testes unitários para os serviços principais.

**Status Atual**: 62 testes passando (0 falhas)
- AuthService: 13 ✅
- ChunkingService: 15 ✅
- EmbeddingService: 19 ✅
- DocumentService: unit ✅
- ChatService: unit ✅
- JwtTokenProvider: unit ✅

#### Executar Todos os Testes
```bash
cd backend
mvn test
```

#### Executar Testes de um Service Específico
```bash
# AuthService tests
mvn test -Dtest=AuthServiceTest

# ChunkingService tests
mvn test -Dtest=ChunkingServiceTest

# EmbeddingService tests
mvn test -Dtest=EmbeddingServiceTest
```

#### Gerar Relatório de Coverage (JaCoCo)
```bash
mvn clean test jacoco:report
```

O relatório será gerado em: `target/site/jacoco/index.html`

**Configuração de Coverage**: Mínimo 70% de cobertura (configurado no pom.xml)

#### Executar Testes com Docker
```bash
docker run --rm -v "$(pwd)":/app \
  maven:3.9-eclipse-temurin-17 \
  bash -c "cd /app && mvn test"
```

### Frontend
```bash
cd frontend
npm test
```

## 🚢 Deploy

### Backend (Railway)

1. Criar projeto no Railway
2. Adicionar PostgreSQL + ativar pgvector
3. Conectar repositório GitHub
4. Configurar variáveis de ambiente:
   - `OPENAI_API_KEY`
   - `JWT_SECRET`
   - `SPRING_PROFILES_ACTIVE=prod`

### Frontend (Vercel)

1. Importar projeto do GitHub
2. Framework: Angular
3. Build Command: `cd frontend && npm install && npm run build`
4. Output Directory: `frontend/dist/helpdesk-ai-frontend/browser`
5. Variável de ambiente: `NG_APP_API_URL` (URL do Railway)

## 📊 Database Schema

### Principais Tabelas

- **users** - Usuários do sistema
- **documents** - Metadados dos PDFs
- **chunks** - Chunks de texto com embeddings (vector(1536))
- **conversations** - Histórico de conversas
- **messages** - Mensagens com citações

### Índice Vetorial

```sql
CREATE INDEX chunks_embedding_idx ON chunks
USING hnsw (embedding vector_cosine_ops);
```

**HNSW** (Hierarchical Navigable Small World) é mais rápido que IVFFlat para < 1M vetores.

## 🔐 Segurança

- Autenticação JWT
- Rate limiting (OpenAI API)
- Validação de MIME types (apenas PDFs)
- Limite de tamanho de arquivo (50MB)
- CORS configurado
- Hashing bcrypt para senhas

## 💰 Custos Estimados

### OpenAI API
- **Embeddings** (text-embedding-3-small): $0.00002 / 1K tokens
  - Exemplo: 100 páginas PDF ≈ $0.003
- **Chat** (GPT-4 Turbo): $0.01 / 1K tokens (input), $0.03 / 1K tokens (output)
  - Exemplo: Pergunta com 5 chunks ≈ $0.045

### Infraestrutura
- Railway (Hobby): $5/mês
- Vercel (Free): $0
- **Total estimado**: $5-10/mês

## 🐛 Troubleshooting

### Docker - Porta 5432 já está em uso
O projeto usa porta `5433` por padrão. Se ainda houver conflito, edite `docker/docker-compose.yml`:
```yaml
ports:
  - "5434:5432"  # Use outra porta
```

### Maven - Dependency resolution failed
Certifique-se de ter acesso ao repositório Spring Milestones:
```bash
mvn clean install -U
```

### pgvector - Extension not found
Verifique se o container está usando a imagem correta:
```bash
docker exec helpdesk-ai-db psql -U postgres -c "SELECT version();"
# Deve mostrar: PostgreSQL 16.x com pgvector
```

## 📚 Recursos e Referências

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [OpenAI API Docs](https://platform.openai.com/docs)
- [Angular Docs](https://angular.io/docs)
- [PrimeNG Components](https://primeng.org/)

## 🗺️ Roadmap

### MVP (Concluído)
- ✅ Setup infraestrutura (PostgreSQL + pgvector)
- ✅ Estrutura de projeto
- ✅ Backend core (ingestão + chat + segurança)
- ⏳ Frontend core (upload + chat UI)
- ✅ Autenticação JWT
- ⏳ Deploy Railway + Vercel

### Futuras Melhorias
- [ ] WebSockets para streaming de respostas
- [ ] Suporte a DOCX, TXT, Markdown
- [ ] OCR para PDFs escaneados
- [ ] Multi-tenancy (workspaces)
- [ ] Analytics dashboard
- [ ] Re-ranking com Cohere API
- [ ] Hybrid search (BM25 + vector)

## 👥 Autor

**Seu Nome**
Desenvolvedor Full-Stack | Java | Angular | Machine Learning

- GitHub: [seu-github](https://github.com/seu-usuario)
- LinkedIn: [seu-linkedin](https://linkedin.com/in/seu-perfil)
- Portfolio: [seu-site](https://seu-portfolio.com)

## 📄 Licença

Este projeto é licenciado sob a licença MIT - veja o arquivo [LICENSE](LICENSE) para detalhes.

---

**Desenvolvido com ☕ Java, 🅰️ Angular e 🤖 OpenAI**
