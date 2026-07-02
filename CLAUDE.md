# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`database-audits-spring-boot` is the **Spring Boot integration** for `database-audits-core`. Core is a library of
database audits (missing primary keys, unindexed/nullable/mistyped foreign keys, redundant indexes, unindexed
`WHERE`/`ORDER BY`/`JOIN` columns, full-table `UPDATE`/`DELETE`, JPA-mapping drift) whose classes carry **no
dependency-injection annotations** on purpose. This module supplies the one piece core deliberately omits: Spring
wiring.

The entire production surface is a single class, `DatabaseAuditTestConfiguration`
(`src/main/java/.../spring/boot/`), a `@TestConfiguration(proxyBeanMethods = false)` that:

- registers every core audit and its collaborators (`JdbcSupport`, `IndexCatalog`, `DatabasePlatform`,
  `QueryPlanExplainer`, `SqlCapturingStatementInspector`) as explicit `@Bean`s, and
- registers the SQL capturer as Hibernate's `StatementInspector` (via a `HibernatePropertiesCustomizer` setting
  `JdbcSettings.STATEMENT_INSPECTOR`), so the runtime audits see every statement the repositories run.

A consumer enables the whole suite with one `@Import(DatabaseAuditTestConfiguration.class)` on a test base class
(assumes a JPA + Liquibase + DataSource test context).

## Build & test

Use the bundled Maven wrapper from this directory. JDK 21 is required.

```powershell
.\mvnw.cmd clean install                                     # unit (surefire, *Test) + integration (failsafe, *IT) + archetype generate-and-run IT
.\mvnw.cmd verify -Dit.test=DatabaseAuditTestConfigurationIT # one integration test (failsafe uses -Dit.test)
.\mvnw.cmd test -Dtest=SomeClassTest#someMethod             # one unit test (surefire uses -Dtest); none exist yet
```

- **Two-module reactor.** The root `pom.xml` is an aggregator over `integration/` (the integration —
  the published production artifact `database-audits-spring-boot-integration`) and `archetype/`
  (`database-audits-spring-boot-archetype`, which scaffolds the example suite into a consumer's project **and** is
  the end-to-end test of the wiring: it generates the sample project and runs it against PostgreSQL). `clean
  install` at the root builds both — `install`, not `verify`, so the archetype's generate-and-run IT resolves
  the freshly built integration artifact from `~/.m2`.
- **The integration module needs no Docker** — its only test, `DatabaseAuditTestConfigurationIT`, boots no Spring
  context (Mockito only). **The archetype module needs Docker**: its `archetype:integration-test` generates the
  sample project and runs the example ITs against a Testcontainers PostgreSQL — the only end-to-end test of the
  Spring wiring.
- **Test console output is redirected to files** (`redirectTestOutputToFile=true`). On failure read
  `target/failsafe-reports/` (integration) or `target/surefire-reports/` (unit), not the build console.
- **Build prerequisite — `database-audits-core:1.0.0-SNAPSHOT` must be resolvable.** It comes from a snapshot repo
  or from `~/.m2`; when working against unreleased core changes, `install` the sibling `../core` first
  (`cd ../core; .\mvnw.cmd install`).
- The Maven parent is declared as `database-audits-parent:1.0.0` with `relativePath ../parent/pom.xml`, but
  `../parent` is a newer `1.0.1-SNAPSHOT`. The version mismatch makes Maven **ignore the relativePath and resolve
  the parent from Maven Central** — editing `../parent` does not affect this build.

## Architecture

### `DatabaseAuditSuite` mirrors core's constructors — keep them in sync

`DatabaseAuditSuite` (`src/main/java/.../spring/boot/`) is the single place that calls core's audit and
collaborator constructors directly: it wires the whole graph for one `(DataSource, EntityManagerFactory)` pair and
exposes each `*AuditAssertion` plus the `DatabaseAuditAssertions` facade. `DatabaseAuditTestConfiguration` builds
one suite from the context's primary `DataSource`/`EntityManagerFactory` and registers its assertions as `@Bean`s
(no component scan). **When a core audit's constructor signature changes (or an audit is added/removed),
`DatabaseAuditSuite` must change in lockstep** — and a new audit also needs its `*AuditAssertion` and a delegating
`@Bean` in the config. This is the spring-boot half of core's standing directive ("when updating any audit class,
also update the spring-boot beans"); a compile failure against core is the intended signal.

To audit **multiple datasources**, a consumer builds one `DatabaseAuditSuite` per datasource from its
`@Qualifier`'d beans in a `@TestConfiguration` named `DatabaseAudit<Name>TestConfiguration`, each exposing its
suite as a `<name>DatabaseAuditSuite` bean. Because peer datasources have no `@Primary`
`DataSource`/`EntityManagerFactory`, the audits resolve each suite **by name** (never by type): in multi mode the
archetype's audit ITs are `@ParameterizedTest`s over the datasource names that inject
`Map<String, DatabaseAuditSuite>` and look up `<name>DatabaseAuditSuite`. When `-DdataSourceNames=Aurora,Reporting`,
`archetype-post-generate.groovy` expands the single `DsNameToken` config token template under `multi/` once per
name and writes a `DatabaseAuditMultiTestConfiguration` aggregator that `@Import`s them all (each audit IT
`@Import`s that aggregator); in multi mode `AbstractDatabaseAuditIT` does **not** import the default
`DatabaseAuditTestConfiguration`, whose by-type injection would be ambiguous. Each audit IT template branches on a
`#set($multi ...)` flag: single-datasource mode is unchanged (individual `*AuditAssertion` beans from the default
config, which keeps its `@Primary` beans), multi mode is the parameterized by-name form. `dataSourceNames` is a
declared property, default `none`.

### Platform detection and the PostgreSQL-only plan audits

`DatabasePlatform` is auto-detected from `DataSource` metadata (`DatabasePlatform.fromDataSource`). The catalog and
JPA audits run on every supported platform; the **plan-based runtime audits** (`WhereClauseIndexAudit`,
`OrderByIndexAudit`, `JoinIndexAudit`) are **PostgreSQL-only and fail fast elsewhere**. On PostgreSQL their
`DataSource` must connect with **`preferQueryMode=simple`** (a PG JDBC property, appended to the JDBC URL):
generic-plan `EXPLAIN` of statements containing `$n` placeholders only works over the simple query protocol. Omit
it and every parameterized statement is skipped, then the audits fail their vacuous-run guard. See the
`DatabaseAuditTestConfiguration` and `QueryPlanExplainer` javadoc.

### The shared-capturer invariant — do not break it

The runtime audits read SQL from a `SqlCapturingStatementInspector`. The **same bean instance** must be both
Hibernate's `StatementInspector` and the instance injected into the audits — that is why the customizer registers
the *bean object*, not its class name. Registering by class name spawns a second capturer Hibernate fills but the
audits never read, silently producing empty captures. Preserve this when editing the config.

### The reusable assertion API — how consumers consume the audits

The integration's published surface is not just the wiring. `…/spring/boot/assertion/` holds one
`<Audit>AuditAssertion` paired with each core audit (plus a `DatabaseAuditAssertions` facade), each registered as a
`@Bean` in `DatabaseAuditTestConfiguration`. An assertion runs its audit and, on violations, throws a plain
`AssertionError` carrying the curated, fix-oriented message and the findings — so a finding is a test **Failure**,
while core's `IllegalStateException` (vacuous capture / unsupported platform) stays an **Error**. No AssertJ in
production. **Consumers `@Autowired` the assertion bean and call `assertClean(schema | excludes…)`** instead of
copying assertion logic or messages. Keep each assertion in lockstep with its audit, the same way the beans are.

### The example suite (generated by the archetype)

The example audit `*IT` classes live as archetype templates under
`archetype/src/main/resources/archetype-resources/src/test/java/` and are generated into a consumer's project (and
run, in-build, by the archetype self-test below). Reuse of the audit logic itself is via the integration's
assertion API; these ITs show how to wire and call it. They mirror core's three audit families — `catalog/`,
`jpa/`, `runtime/` — and demonstrate the consumer contract:

- Each example `extends AbstractDatabaseAuditIT` (`@SpringBootTest` + `@Import(DatabaseAuditTestConfiguration.class)`),
  `@Autowired`s the audit **assertion** beans and calls `assertClean(…)`, reading the schema via
  `@Value("${database.datasource.schema-name}")`.
- **Runtime audit ITs are annotated `@Order(Integer.MAX_VALUE)`** (JUnit's `org.junit.jupiter.api.Order`) so they
  run *after* the SQL is captured. What actually enforces order is the priming test `RepositoryWorkloadIT`
  (`@Order(Integer.MIN_VALUE)`, runs first, via `junit-platform.properties` enabling
  `ClassOrderer$OrderAnnotation`). **All example classes must use JUnit's `@Order`, not Spring's.**
- The JPA example runs under the default `ddl-auto=none`: `SchemaEntityValidationAudit` walks Hibernate's entity
  mappings against the live schema and reports every mismatch in one run, instead of relying on Hibernate's
  fail-fast `ddl-auto=validate` startup check (which aborts on the first mismatch).
- Most show `EXCLUDED_*` exclusion constants (relations, SQL fragments, columns, indexes, statements) — the
  intended way for consumers to suppress known/intentional violations instead of weakening the audit.
  `ForeignKeyIndexAuditIT` and `PrimaryKeyPresenceAuditIT` call the no-exclusion overload directly.

The **demo harness** that makes the examples run — `DemoApplication`, `app/*` (entities + repositories +
`DemoPostgresTestConfig`), `runtime/RepositoryWorkloadIT`, and `src/test/resources/` (`application.properties` + the Liquibase
XML changelog) — is generated alongside the audit ITs. `DemoPostgresTestConfig` starts one shared PostgreSQL
container and registers its JDBC URL (carrying `preferQueryMode=simple`) through a `DynamicPropertyRegistrar`.

When you change a core audit's public `audit(...)` signature, update the archetype templates
(`archetype/src/main/resources/archetype-resources/…`) so the documented usage stays correct.

### `archetype/` — generating and verifying the suite

`database-audits-spring-boot-archetype` generates the example suite above into a consumer's project — with package,
coordinates, schema name, and Postgres image filled in at generation time — and is also the in-build end-to-end
test of the wiring. Generate with:

```powershell
mvn archetype:generate -DinteractiveMode=false `
  -DarchetypeGroupId=io.github.database-audits `
  -DarchetypeArtifactId=database-audits-spring-boot-archetype `
  -DarchetypeVersion=1.0.0-SNAPSHOT `
  -DgroupId=com.example -DartifactId=demo -Dpackage=com.example.demo
```

- **Templates** live under `archetype/src/main/resources/archetype-resources/` and are filtered by Velocity at
  generation. Literal `$` characters that must survive into generated sources (Spring `@Value("${…}")` placeholders
  and `ClassOrderer$OrderAnnotation`) are emitted using Velocity's `#[[ ]]#` raw block: `#[[${]]#` produces `${`
  and `#[[$]]#` produces `$`.
- **Properties** (`META-INF/maven/archetype-metadata.xml`): `generateMode` (`project`), `schemaName` (`public`),
  `schemaPropertyName` (`database.datasource.schema-name`), `parentClass` (`none`), `databaseAuditsVersion`
  (`1.0.0-SNAPSHOT`), `springBootVersion` (`4.1.0`), `postgresImage` (`postgres:16`), `disabledTests` (`false`,
  annotates every generated test method with JUnit's `@Disabled` when `true`).
  All have defaults; none are required. The self-test's `archetype.properties` must list every property that the
  templates reference — the IT mojo does not apply defaults the way `archetype:generate` does.
- **`projectDirectory` is NOT a declared archetype property** (deliberately). `archetype:generate` therefore
  drops it from the post-generate script's `request.properties`; on the command line it arrives **only as a JVM
  system property**.
  So `archetype-post-generate.groovy` reads it from `request.properties` first (the integration-test mojo path,
  which supplies it via `archetype.properties`) and falls back to `System.getProperty('projectDirectory')` (the
  command-line path). Relative values (including `.`) resolve against the shell working directory — `PWD` env
  first (immune to `System.setProperty`), then `user.dir`. Empty/absent ⇒ files land under `-DoutputDirectory`.
- **`tests-only` and existing projects**: Two constraints apply. (1) `archetype:generate` refuses to use any
  directory containing a `pom.xml` as `outputDirectory`. (2) Running from inside a Maven project causes the
  plugin to try adding the generated artifact as a `<module>`, which fails if packaging is not `pom`. Fix: run
  the command from a directory with no `pom.xml` (home dir, `/tmp`, etc.); set `-DoutputDirectory` to a scratch
  location and `-DprojectDirectory` to your real project root — an absolute path, or a path relative to the
  directory you run the command from (`.` for the current directory).
- **Self-test.** The `maven-archetype` lifecycle runs `archetype:integration-test` during `verify`/`install`: it
  generates the project under `src/test/resources/projects/full/` and runs its `verify` (Testcontainers PostgreSQL,
  Docker required), exercising generation, compilation, and the audits on every build. The module skips the
  inherited surefire/failsafe (it has no tests of its own; the generated project's compiled tests live under
  `target/test-classes/projects`).
- The generated pom depends on `database-audits-spring-boot-integration` and configures `maven-failsafe-plugin`
  itself (it uses `spring-boot-starter-parent`, not `database-audits-parent`).

## CI / release (GitHub Actions)

- **build-any-branch.yml** — `clean install` on every push/PR (skips doc-only changes); JDK via the local
  `.github/actions/jdk-setup` composite (Temurin 21 + Maven cache).
- **deploy-snapshot.yml** — after a green `main` build, deploys the `-SNAPSHOT` to Maven Central.
- **release.yml** — triggered by a `v*` tag (created by `maven-release-plugin`); deploys `-Prelease` (GPG-signed,
  with sources + javadoc).
- **publish-docs.yml** — builds the Maven site + JaCoCo reports and deploys to GitHub Pages.

## Conventions (shared across the database-audits project)

- **Branch, don't commit to `main`.** Create/switch to a topic branch for any change.
- **Tests:** `<ClassName>Test` = unit (surefire), `<ClassName>IT` = integration (failsafe), `<ClassName>AT` =
  acceptance. Method names: `test<MethodName>_<StartingStateConditions>_<AssertedOutcome>`. Prefer AssertJ, assert whole
  objects over field-by-field, and add `.as("…")` failure descriptions ending with a period.
- **Code style:** favor immutability and constructor injection (tests use field injection); avoid
  `utils`/`helper`/`support` package or class names; remove blank lines after an opening brace; write JavaDoc on
  public classes/methods as complete sentences. Lombok and JSpecify (`@Nullable`) are available.
- **Commits:** Conventional Commits, atomic. Types `feat|fix|docs|refactor|test|build|ci`; scopes include the DB
  names plus `core`, `spring`, `pom`, `site`, `database`, `metadata`, `scripts`. Capitalize the first word after
  the scope; use `*` for bullets; reference issues in the footer as `Refs: 123` (no `#`). Use `git mv` to rename.
- **Audits return findings; callers assert.** Each core audit's `audit(...)` returns a `List<String>` of
  violations (empty = clean); in this module the `*AuditAssertion` beans surface that as
  `assertClean(schema | excludes…)`. AssertJ is test-scoped in core, not a production dependency.

## Jackknife (`.jackknife/`)

Workspace for the jackknife-maven-plugin: decompiled dependency source under `.jackknife/source/`, class manifests
under `.jackknife/manifest/`, and method instrumentation for debugging. It is generated — **do not hand-edit**. To
inspect/decompile/find a dependency class, run `.\mvnw.cmd jackknife:index` (run any `jackknife:*` goal immediately
without asking) and follow `.jackknife/USAGE.md`.

## Claude Directives

- Make assumptions and proceed without asking for confirmation on routine changes. If an action is destructive (e.g., deleting files), pause and ask.
- To start docker, run "Rancher Desktop".
- Keep the archetype generated classes as pure as possible, not using Lombok
- Keep the archetype generated classes simple as possible, only using JUnit and this product's classes and not using other library classes such as Lombok.  Ask before adding a non-JDK class usage to an archetype generated class.

