# Security Policy

**Project:** Minab Banking  
**Maintainer:** Ali Behzadian / OpenFinova  
**Contact:** maintainers@openfinova.com

---

## Supported Versions

Only the latest version on the `main` branch receives security updates.

| Branch | Supported |
|--------|-----------|
| `main` | ✅ Yes |
| `dev`  | ⚠️ Development only — not for production use |
| Others | ❌ No |

---

## Reporting a Vulnerability

We take security vulnerabilities seriously — especially given the financial nature of
this project. We appreciate responsible disclosure and will do our best to address
reported issues promptly.

### Private Reporting (Preferred)

Use GitHub's private vulnerability reporting feature:

```
GitHub → Security → Advisories → Report a vulnerability
```

This is our preferred channel. Your report is visible only to the maintainers and
keeps the vulnerability confidential until a fix is released.

### Email Reporting

If you prefer email, send your report to:

```
maintainers@openfinova.com
```

Please use the subject line: `[SECURITY] <brief description>`

---

## What to Include in Your Report

Please provide as much of the following as possible:

- **Description** — a clear explanation of the vulnerability
- **Impact** — what an attacker could achieve by exploiting it
- **Affected component** — which service, module, or endpoint is affected
- **Steps to reproduce** — a minimal, reproducible example
- **Suggested fix** — if you have one (optional but appreciated)
- **CVE reference** — if one already exists

---

## Response Timeline

| Stage | Timeframe |
|-------|-----------|
| Initial acknowledgement | Within 48 hours |
| Vulnerability assessment | Within 7 days |
| Fix development | Depends on severity (see below) |
| Public disclosure | After fix is released |

### Fix Timeline by Severity

| Severity | Target fix time |
|----------|----------------|
| Critical | 24–48 hours |
| High | 7 days |
| Medium | 30 days |
| Low | 90 days |

---

## Disclosure Policy

We follow the principle of **responsible disclosure**:

1. You report the vulnerability privately
2. We confirm and assess the issue
3. We develop and test a fix
4. We release the fix
5. We publish a security advisory crediting the reporter (unless you prefer to remain anonymous)
6. You are welcome to publish your own write-up after the advisory is public

We ask that you **do not publicly disclose** the vulnerability until we have released
a fix or we have mutually agreed on a disclosure date.

---

## Scope

### In Scope

The following are within scope for vulnerability reports:

- Authentication and authorisation bypasses
- SQL injection or other injection attacks
- Broken access control
- Sensitive data exposure
- Security misconfiguration
- Cryptographic weaknesses
- Business logic flaws with financial impact
- Insecure direct object references (IDOR)
- JWT token vulnerabilities
- API security issues

### Out of Scope

The following are **not** in scope:

- Vulnerabilities in third-party dependencies (report these upstream; we handle them via Dependabot)
- Social engineering attacks
- Physical security attacks
- Denial of service (DoS) attacks
- Issues in `dev` branch that do not affect `main`
- Issues requiring physical access to a device
- Theoretical vulnerabilities with no practical exploit

---

## Safe Harbour

We will not pursue legal action against security researchers who:

- Report vulnerabilities through our private disclosure process
- Act in good faith and do not exploit vulnerabilities beyond what is necessary to demonstrate the issue
- Do not access, modify, or delete data belonging to other users
- Do not disrupt our services

---

## Security Measures

This project implements the following security controls:

- 🔒 Signed commits required on `main`
- 🔒 Branch protection with required CI checks
- 🔒 Dependabot automated dependency updates
- 🔒 CodeQL static analysis on every PR
- 🔒 Secret scanning with push protection
- 🔒 Dependency vulnerability review on every PR
- 🔒 Contributor License Agreement (CLA) required
- 🔒 Private vulnerability reporting enabled

---

## Security Hall of Fame

We thank the following researchers for responsibly disclosing vulnerabilities:

*No reports received yet — you could be the first!*

---

*This security policy is based on industry best practices for open source financial software.*