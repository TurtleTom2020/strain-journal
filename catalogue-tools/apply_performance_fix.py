from pathlib import Path
import shutil, sys
source = Path(__file__).resolve().parent.parent / "performance-fix-v0.55.1"
target = Path(sys.argv[1])
for file in source.rglob("*"):
    if file.is_file():
        destination = target / file.relative_to(source)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(file, destination)
print("Applied v0.55.1 company lookup and UI performance fixes")
