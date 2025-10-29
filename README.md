# Descobre-IP

## Visão Geral
O **Descobre-IP** é uma aplicação desenvolvida em **Java 17** com **Spring Boot 3.5.7**.  
O objetivo do projeto é realizar **varreduras SNMP** em uma rede para descobrir dispositivos, interfaces e IPs, e registrar essas informações na **API da NetBox**.

O projeto utiliza **programação reativa** com **Spring WebFlux**, **Flux** e **Mono**, além de **WebClient** para comunicação assíncrona com a API da NetBox.

---

## Funcionalidades
- Varredura de faixas de IP para descobrir dispositivos SNMP ativos.
- Registro de **dispositivos**, **interfaces** e **endereços IP** na NetBox.
- Tratamento de exceções personalizadas e validações.
- Integração reativa e eficiente para redes grandes.

---

## Tecnologias Utilizadas
- **Java 17**
- **Spring Boot 3.5.7**
- **Spring WebFlux**
- **WebClient**
- **Lombok**
- **Maven**
- **NetBox Java Client**
- **Docker/Docker Compose**

---
## Atenção ⚠️

O projeto foi mais testado em nuvem do que usando Docker Compose localmente.
É recomendado rodar em nuvem para evitar problemas de dependências e performance.

## Configuração

O projeto utiliza um arquivo `.env` para definir variáveis de ambiente essenciais, incluindo o **Token da API da NetBox** e a **URL do servidor**.
Caso seja necessário para usar a API Netbox em nuvem, consulte as credenciais no `.env`

## Executando o Projeto
1. Compilar o projeto
Para compilar o projeto e limpar builds antigos:

-- *mvn clean package*

2. Rodar a aplicação

Para executar a aplicação localmente:

-- *mvn spring-boot:run*

