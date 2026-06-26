| METHOD | PATH | ALLOWED_ROLES |
| --- | --- | --- |
| GET | /admin | ADMIN |
| POST | /admin | ADMIN |
| DELETE | /any-method | ADMIN |
| GET | /any-method | ADMIN |
| HEAD | /any-method | ADMIN |
| OPTIONS | /any-method | ADMIN |
| PATCH | /any-method | ADMIN |
| POST | /any-method | ADMIN |
| PUT | /any-method | ADMIN |
| GET | /any-role | {ANY_ROLE} |
| GET | /authenticated | {AUTHENTICATED} |
| GET | /gone | USER |
| GET | /guest-only |  |
| DELETE | /items/{id} | ADMIN |
| GET | /items/{id} | ADMIN |
| GET | /locked |  |
| GET | /not-found | USER |
| GET | /public | {⚠ PERMIT_ALL ⚠} |
| GET | /server-error | USER |
| GET | /user | {ANY_ROLE} |
