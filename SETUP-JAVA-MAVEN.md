# Guia de Instalação: Java 17 + Maven

## 🔴 Status Atual

- ❌ **Java**: Você tem Java 8, mas precisa de Java 17+
- ❌ **Maven**: Não instalado

## 📦 Instalação

### Opção 1: Instalação Manual (Recomendado)

#### 1. Instalar Java 17 (Eclipse Temurin)

**Baixar:**
1. Acesse: https://adoptium.net/temurin/releases/
2. Selecione:
   - **Version**: 17 (LTS)
   - **Operating System**: Windows
   - **Architecture**: x64
   - **Package Type**: JDK
   - **Image Type**: JRE or JDK (escolha JDK)
3. Baixe o instalador `.msi`

**Instalar:**
1. Execute o instalador
2. **IMPORTANTE**: Marque a opção "Set JAVA_HOME variable"
3. **IMPORTANTE**: Marque a opção "Add to PATH"
4. Finalize a instalação

**Verificar:**
Abra um **novo** terminal (PowerShell ou CMD) e execute:
```bash
java -version
```
Deve mostrar algo como: `openjdk version "17.0.x"`

#### 2. Instalar Maven 3.9+

**Baixar:**
1. Acesse: https://maven.apache.org/download.cgi
2. Baixe `apache-maven-3.9.9-bin.zip` (Binary zip archive)

**Instalar:**
1. Extraia o ZIP para `C:\Program Files\Apache\maven` (crie as pastas se necessário)
2. Adicione ao PATH:
   - Abra "Editar as variáveis de ambiente do sistema"
   - Clique em "Variáveis de Ambiente"
   - Em "Variáveis do sistema", encontre `Path` e clique "Editar"
   - Clique "Novo" e adicione: `C:\Program Files\Apache\maven\bin`
   - Clique "OK" em todas as janelas

3. (Opcional) Criar variável `M2_HOME`:
   - Em "Variáveis do sistema", clique "Novo"
   - Nome: `M2_HOME`
   - Valor: `C:\Program Files\Apache\maven`

**Verificar:**
Abra um **novo** terminal e execute:
```bash
mvn -version
```
Deve mostrar algo como: `Apache Maven 3.9.9`

---

### Opção 2: Instalação via Chocolatey (Mais Rápido)

Se você tem o [Chocolatey](https://chocolatey.org/) instalado:

```bash
# Instalar Java 17
choco install temurin17 -y

# Instalar Maven
choco install maven -y

# Reabrir terminal e verificar
java -version
mvn -version
```

---

### Opção 3: Instalação via Scoop

Se você tem o [Scoop](https://scoop.sh/) instalado:

```bash
# Instalar Java 17
scoop bucket add java
scoop install temurin17-jdk

# Instalar Maven
scoop install maven

# Verificar
java -version
mvn -version
```

---

## ✅ Após a Instalação

### 1. Verificar Instalação

Abra um **novo terminal** (importante!) e execute:

```bash
# Deve mostrar Java 17.x
java -version

# Deve mostrar Maven 3.9.x
mvn -version

# Deve mostrar o caminho correto
echo %JAVA_HOME%
```

### 2. Testar Compilação do Projeto

```bash
cd C:\Users\PICHAU\Documents\Dev\helpdesk-ai\backend
mvn clean compile
```

**Resultado esperado:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX s
```

### 3. Se houver erros de dependências

```bash
# Limpar cache do Maven
mvn clean install -U

# Ou forçar download de dependências
mvn dependency:purge-local-repository
mvn clean install
```

---

## 🐛 Troubleshooting

### Erro: "JAVA_HOME não está definido"

**Windows (PowerShell como Admin):**
```powershell
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.x", [System.EnvironmentVariableTarget]::Machine)
```

Ajuste o caminho `C:\Program Files\Eclipse Adoptium\jdk-17.0.x` para onde o Java foi instalado.

### Erro: "mvn não é reconhecido"

Verifique se `C:\Program Files\Apache\maven\bin` está no PATH:
```bash
echo %PATH%
```

Se não estiver, adicione manualmente (veja instruções acima).

### Java 8 ainda está sendo usado

Se após instalar o Java 17, o comando `java -version` ainda mostra Java 8:

1. Verifique qual Java está sendo usado:
   ```bash
   where java
   ```

2. Se aparecer múltiplos caminhos, o Java 8 está antes no PATH
3. Edite o PATH e mova o Java 17 para o **topo** da lista

---

## 📚 Links Úteis

- **Java 17 (Adoptium)**: https://adoptium.net/
- **Maven Download**: https://maven.apache.org/download.cgi
- **Maven Getting Started**: https://maven.apache.org/guides/getting-started/
- **Spring Boot com Maven**: https://spring.io/guides/gs/maven/

---

## 🚀 Depois de Instalar

Volte ao Claude Code e diga **"Instalação concluída"** para continuar com os testes!

Vamos compilar o projeto e verificar se está tudo OK antes de avançar para os Services.
