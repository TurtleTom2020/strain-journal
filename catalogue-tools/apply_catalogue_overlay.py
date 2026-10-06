"""Apply the reviewed catalogue-only overlay to the original Android project."""
import base64,hashlib,io,json,pathlib,sys,zipfile
root=pathlib.Path(__file__).resolve().parent.parent
manifest=json.loads((root/'catalogue-overlay-v0.55.0/manifest.json').read_text())
project=pathlib.Path(sys.argv[1]).resolve()
encoded=''.join((root/'catalogue-overlay-v0.55.0'/name).read_text().strip() for name in manifest['parts'])
payload=base64.b64decode(encoded,validate=True)
assert hashlib.sha256(payload).hexdigest()==manifest['sha256'],'Overlay checksum mismatch'
protected=['app/stable-debug.keystore','app/src/main/java/com/medleaf/journal/data/AppDatabase.kt','app/src/main/java/com/medleaf/journal/data/JournalDao.kt','app/src/main/java/com/medleaf/journal/data/Models.kt','app/src/main/java/com/medleaf/journal/data/JournalRepository.kt']
before={p:(project/p).read_bytes() for p in protected}
with zipfile.ZipFile(io.BytesIO(payload)) as archive:
 for name in archive.namelist():
  path=pathlib.PurePosixPath(name)
  assert not path.is_absolute() and '..' not in path.parts,name
  assert name in manifest['files'] and name not in protected,name
  dest=project/name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(archive.read(name))
for name,original in before.items():assert (project/name).read_bytes()==original,'Protected file changed: '+name
assert 'versionName = "0.55.0"' in (project/'app/build.gradle.kts').read_text()
print('Applied reviewed catalogue expansion; signing key, database and review code retained.')
