# AR 3D Graph Phase 2C — Endurance and Resources

Result: **No leak signal on available paths; live AR GPU/session endurance blocked**

## Workload completed

| Workload | Count | Evidence |
| --- | ---: | --- |
| Successful graph generations through UI | 50 | 25 initial + 25 corrected retries |
| Invalid generation attempts | 25 | Controlled validation |
| Anchor placements/replacements | 101 | Controlled anchor owner; one initial + 100 replacements |
| Reset Placement UI actions | 25 | Graph data preserved |
| Clear UI actions | 25 | Graph state/resources cleared |
| Background/resume | 10 | Actual installed app |
| Orientation changes | 10 | Actual installed app |
| Verified AR exits/re-entries | 10 | Semantic route verification |
| Original 3D operations | 60 | Connected UI test |
| Five-workspace navigation operations | 200 | 20 cycles × five open/back pairs |

## Memory observations

| Point | PSS | RSS |
| --- | ---: | ---: |
| Initial unsupported AR screen | 138,773 KB | 256,208 KB |
| After lifecycle/re-entry endurance while in app | 148,557 KB | 271,504 KB |
| After final AR exit | 152,382 KB | 275,632 KB |
| After background `COMPLETE` trim | 145,521 KB | 268,780 KB |

The trim recovered 6,861 KB PSS. Remaining growth versus the initial sample was 6,748 KB and was not monotonic evidence of an AR leak; the test exercised many app workspaces and Compose/runtime caches while no ARCore session, camera or GPU scene existed. No camera client or active AR anchor was observable after exit.

Real camera buffers, GL frame workload, native ARCore memory, real anchor count, GPU buffers and supported-session instances cannot be measured on this AVD and remain blocked.
