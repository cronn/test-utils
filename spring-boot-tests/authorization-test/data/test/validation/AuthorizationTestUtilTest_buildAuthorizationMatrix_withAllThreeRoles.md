| METHOD | PATH | ALLOWED_ROLES |
| --- | --- | --- |
| GET | /actuator/info | {⚠ PERMIT_ALL ⚠} |
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
| DELETE | /error | {⚠ PERMIT_ALL ⚠} |
| GET | /error | {⚠ PERMIT_ALL ⚠} |
| HEAD | /error | {⚠ PERMIT_ALL ⚠} |
| OPTIONS | /error | {⚠ PERMIT_ALL ⚠} |
| PATCH | /error | {⚠ PERMIT_ALL ⚠} |
| POST | /error | {⚠ PERMIT_ALL ⚠} |
| PUT | /error | {⚠ PERMIT_ALL ⚠} |
| GET | /gone | USER |
| GET | /guest-only | GUEST |
| DELETE | /items/{id} | ADMIN |
| GET | /items/{id} | ADMIN |
| GET | /locked |  |
| GET | /not-found | USER |
| GET | /public | {⚠ PERMIT_ALL ⚠} |
| GET | /server-error | USER |
| GET | /user | ADMIN<br>USER |
