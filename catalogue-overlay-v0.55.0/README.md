# Terpfolio 0.55.0 catalogue expansion

The Android build unpacks the existing v0.54.3 source archive and applies this checksummed, catalogue-only overlay. The source artifact produced by Actions contains the complete expanded project.

- 977 imported flower records from 1,182 public flower listings and explicit historical UK directory cultivation statements.
- Original rich records and supplier-specific growers/genetics/terpenes are retained. Imported product codes become searchable aliases.
- 33 additional visually reviewed company logos, with source URLs in the expanded project's catalogue/logo-sources.json.
- 63 bundled company logos in total. Cultivator remains on the left; a different brand remains on the right. Monochrome new logos follow the selected theme colour.
- Unknown growers and unavailable authentic logos retain text fallback. No unverified logo is assigned to Thunderchild Cultivation LP.
- The signing key, application ID, database schema, review/photo models and repository code are unchanged. Version code increases to 58.

The chunks concatenate to a base64 ZIP. manifest.json provides its SHA-256 checksum and exact changed-file list. catalogue-tools/apply_catalogue_overlay.py validates and applies it while checking protected files byte-for-byte. Unit tests run before the APK is uploaded.

Source catalogue retrieval: 2026-10-06. Listings include historic products and are not a live stock guarantee; batch THC/CBD stays on each review.
