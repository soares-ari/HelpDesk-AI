# Backend Compilation Issues

## Status
Backend código está **95% completo** mas há problemas de compilação relacionados ao **Lombok annotation processing** no Windows com Maven.

## Problemas Identificados

### 1. Lombok Annotation Processor não está sendo executado
**Sintomas:**
- Erros de compilação: `cannot find symbol: variable log`
- Erros de compilação: `cannot find symbol: method getId()` (getters/setters)
- Erros de compilação: `cannot find symbol: method builder()` (@Builder)

**Causa:**
O Maven compiler plugin não está processando corretamente as anotações do Lombok (`@Slf4j`, `@Getter`, `@Setter`, `@Builder`, etc.) no ambiente Windows.

### 2. Conflito de Import PGvector
**Status:** ✅ **CORRIGIDO**
- Imports incorretos `org.pgvector.PGvector` foram corrigidos para `com.pgvector.PGvector`

### 3. Conflito de Nome ChatResponse
**Status:** ✅ **CORRIGIDO**
- Conflito entre `org.springframework.ai.chat.model.ChatResponse` e `com.helpdeskai.dto.ChatResponse` foi resolvido usando fully qualified name

### 4. Duplicação de Construtor em EmbeddingException
**Status:** ✅ **CORRIGIDO**
- Construtor duplicado foi removido

## Soluções Possíveis

### Opção 1: Configurar Annotation Processor Path (Tentada - Falhou)
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>17</source>
        <target>17</target>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

**Resultado:** `java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`

### Opção 2: Usar IDE (IntelliJ IDEA ou Eclipse) - **RECOMENDADO**
1. Importar projeto Maven no IntelliJ IDEA ou Eclipse
2. Instalar Lombok Plugin na IDE
3. Habilitar annotation processing nas configurações
4. Rebuild project

**IntelliJ IDEA:**
- Settings → Plugins → Install "Lombok"
- Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing
- Build → Rebuild Project

**Eclipse:**
- Install Lombok: https://projectlombok.org/setup/eclipse
- Project → Clean
- Project → Build

### Opção 3: Atualizar Maven e Java
```bash
# Verificar versões atuais
java -version  # Deve ser Java 17+
mvn -version   # Deve ser Maven 3.8+

# Atualizar se necessário
```

### Opção 4: Usar Maven Wrapper (mvnw)
Criar wrapper do Maven para garantir versão consistente:
```bash
mvn wrapper:wrapper -Dmaven=3.9.6
./mvnw clean install
```

### Opção 5: Compilar sem Lombok (Último Recurso)
Adicionar getters/setters manualmente em todas as entidades e DTOs (não recomendado - ~500 linhas extras de código)

## Workaround Temporário

### Para testar sem compilar localmente:
1. Usar Docker para compilar:
```bash
docker run -it --rm -v "$(pwd)":/app -w /app maven:3.9-eclipse-temurin-17 mvn clean package
```

2. Ou usar GitHub Actions para build automatizado

## Arquivos Afetados

### Dependem de Lombok @Slf4j (log variable):
- `AsyncConfig.java` (47, log.info)
- `SecurityConfig.java` (77, log.info)
- `JwtAuthenticationFilter.java` (65, 69, log.debug/error)
- `JwtTokenProvider.java` (57, 95, 97, 99, 101, 103, 129, 144, log.debug/error)
- `AuthController.java` (40, 44, log.info)
- `DocumentController.java` (múltiplas linhas)
- `ChatController.java` (múltiplas linhas)
- `ChatService.java` (73, 74, 89, 99, 118, 237, log.info/debug/error)
- Todos os Services (ChunkingService, EmbeddingService, DocumentService, AuthService)

### Dependem de Lombok @Getter/@Setter/@Builder:
- Todas as Entidades (User, Document, Chunk, Conversation, Message)
- Todos os DTOs (7 classes)

## Próximos Passos Recomendados

1. **Usar IntelliJ IDEA** para compilar e executar (mais rápido)
2. **Ou** configurar Docker build
3. **Ou** investigar conflito entre Maven/Java/Lombok no Windows
4. Após compilar: executar testes de integração
5. Após testes: fazer deploy

## Informações do Ambiente

**Java Version:**
```
java version "17.x.x" (verificar com java -version)
```

**Maven Version:**
```
Apache Maven 3.x.x (verificar com mvn -version)
```

**Lombok Version:** 1.18.32 (definido no pom.xml via Spring Boot parent)

**OS:** Windows (baseado nos paths C:\Users\PICHAU\...)

## Referências

- Lombok Setup: https://projectlombok.org/setup/maven
- Spring Boot + Lombok: https://www.baeldung.com/lombok-ide
- Maven Compiler Plugin: https://maven.apache.org/plugins/maven-compiler-plugin/

---

**Data:** 2025-12-04
**Status:** Aguardando solução de configuração Lombok
**Próxima Ação:** Usar IDE ou Docker para compilar
