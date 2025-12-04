# Corrigir JAVA_HOME para Java 25

## 🔴 Problema Detectado

- **JAVA_HOME atual**: `C:\Program Files\Java\jdk-16.0.1\`
- **Java instalado**: Java 25 (Temurin)
- **Java necessário**: Java 17+ (Java 25 funciona perfeitamente)

O Maven está usando o Java 16 porque `JAVA_HOME` está apontando para ele.

## ✅ Solução

### Opção 1: Via Interface Gráfica (Mais Fácil)

1. Pressione `Windows + R`
2. Digite: `sysdm.cpl` e pressione Enter
3. Vá para a aba **"Avançado"**
4. Clique em **"Variáveis de Ambiente"**
5. Em **"Variáveis do sistema"**, procure por `JAVA_HOME`
6. Clique em **"Editar"**
7. Altere o valor para: `C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot`

   **NOTA**: O caminho pode ser ligeiramente diferente. Para encontrar o caminho correto:
   - Abra o **Explorador de Arquivos**
   - Vá para `C:\Program Files\Eclipse Adoptium\`
   - Procure pela pasta que começa com `jdk-25`
   - Copie o caminho completo

8. Clique **"OK"** em todas as janelas
9. **IMPORTANTE**: Feche e abra um NOVO terminal

### Opção 2: Via PowerShell (Admin)

Abra **PowerShell como Administrador** e execute:

```powershell
# Listar instalações de Java
ls "C:\Program Files\Eclipse Adoptium"
ls "C:\Program Files\Java"

# Encontre a pasta jdk-25.x.x-hotspot e copie o caminho
# Depois configure JAVA_HOME (ajuste o caminho se necessário)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot", [System.EnvironmentVariableTarget]::Machine)
```

### Opção 3: Via PowerShell (Temporário - apenas para este projeto)

Se você não quer alterar o JAVA_HOME global (para não afetar outros projetos):

```powershell
# Abra PowerShell normal (não precisa ser Admin)
cd C:\Users\PICHAU\Documents\Dev\helpdesk-ai\backend

# Configure JAVA_HOME temporariamente
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"

# Verifique
echo $env:JAVA_HOME

# Compile o projeto
mvn clean compile
```

**NOTA**: Esta opção 3 só funciona no terminal atual. Se fechar o terminal, precisa configurar novamente.

---

## 🔍 Encontrar o Caminho Correto do Java 25

Execute este comando no PowerShell para encontrar automaticamente:

```powershell
# Listar todas as versões do Java
ls "C:\Program Files\Eclipse Adoptium" | Select Name
ls "C:\Program Files\Java" | Select Name

# Verificar qual Java está em uso
java -version
```

O caminho provavelmente é um desses:
- `C:\Program Files\Eclipse Adoptium\jdk-25.0.1+8-hotspot`
- `C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot`
- `C:\Program Files\Eclipse Foundation\jdk-25.0.1+8-hotspot`

---

## ✅ Verificar se Funcionou

Depois de configurar, **feche e abra um NOVO terminal** e execute:

```bash
# Verificar JAVA_HOME
echo %JAVA_HOME%

# Verificar versão do Java
java -version

# Verificar versão que Maven vai usar
mvn -version
```

Todos os comandos devem mostrar **Java 25** (ou pelo menos Java 17+).

---

## 🚀 Depois de Corrigir

Volte ao Claude Code e diga **"JAVA_HOME corrigido"** para testarmos a compilação novamente!
