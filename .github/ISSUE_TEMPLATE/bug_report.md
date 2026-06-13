---
name: Bug report
about: Create a report to help us improve
title: ''
assignees: ''
type: Bug

---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. database-audits-spring-boot version
2. Database server and version
3. How the audits are wired (Spring Boot version + `DatabaseAuditTestConfiguration`, another DI container, or manual construction)
4. Which audit and the `assertClean(...)` arguments used (excluded relations / SQL fragments / columns / indexes / statements)
5. The failure message — for the runtime audits, include the offending captured SQL and EXPLAIN output if available

**Expected behavior**
A clear and concise description of what you expected to happen.

**Additional context**
Add any other context about the problem here.
