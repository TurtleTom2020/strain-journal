import urllib.request,urllib.parse,json,pathlib,re,concurrent.futures,html
from html.parser import HTMLParser
OUT=pathlib.Path('source-export/logos'); OUT.mkdir(parents=True,exist_ok=True)
SITES={'Thunderchild Cultivation LP':'https://licensedproducerscanada.ca/listings/thunderchild-cultivation','Herdade das Barrocas':'https://cannabis.hbarrocas.com/','PurpleFarm':'https://purplefarm.co/','LOT420':'https://www.lot420.com/','Khiron':'https://khironmed.com/','Lumir':'https://lumirclinic.com/','Hexacan':'https://hexacan.com/','Noidecs':'https://noidecs.com/','SafriCanna':'https://safricanna.com/','Dalgety':'https://dalgetyuk.co.uk/','Hilltop Leaf':'https://hilltopleaf.com/','Little Green Pharma':'https://www.littlegreenpharma.com/','ANTG':'https://australiannatural.com/','Cantourage':'https://www.cantourage.com/','EastCann':'https://eastcann.ca/','Next Friday':'https://nextfriday.com/','Origine Nature':'https://originenature.com/','Cake & Caviar':'https://cakeandcaviar.ca/','Common Roots':'https://commonrootscannabis.com/','CannFX':'https://cannfx.com/','Cookies':'https://cookies.co/','Qwest':'https://qwestcannabis.com/','Decibel':'https://decibelcc.com/','Rubicon Organics':'https://www.rubiconorganics.com/','SNDL':'https://sndl.com/','AgMedica':'https://agmedica.ca/','Miracle Valley':'https://miraclevalley.ca/','Truro':'https://trurocannabis.com/','Tyson 2.0':'https://tyson20.com/','MedCan':'https://medcan.co.za/','ECS Botanics':'https://www.ecsbotanics.com.au/','GreenSeal':'https://greensealcannabis.ca/','Avant':'https://avantbrands.com/','Cielo Verde':'https://cieloverde.ca/','KRFT':'https://krft.ca/','Northern Green Canada':'https://www.northerngreencanada.com/','PharmaCann':'https://pharmacann.com.mk/','Redecan':'https://redecan.ca/','Sundaze':'https://sundaze.com.au/','Cannada Craft':'https://cannadacraft.com/','Kootenay Quantum':'https://kootenayquantum.com/','Sitka Legends':'https://sitkalegends.com/','BC Green':'https://bcgreen.ca/','Four20 Pharma':'https://www.420pharma.de/','Avaay Medical':'https://avaay.de/','Enua':'https://enua.de/','Tilray':'https://tilraymedical.com/','Therismos':'https://therismos.com/','Green Joÿ':'https://greenjoy.ca/','J.R. Strain':'https://jrstrain.ca/','Spirit Bear':'https://spiritbearcannabis.com/','HighGreens':'https://highgreens.com/','Island Canna':'https://islandcanna.ca/','British Cannabis':'https://britishcannabis.org/','Althea':'https://althea.life/','Kanabo':'https://kanabogroup.com/','GrowLab Organics':'https://growlaborganics.com/','Canopy Growth':'https://www.canopygrowth.com/','Muzo':'https://muzo.ca/','Wellford':'https://wellford.com/'}
def get(url):
 req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0','Accept':'*/*'})
 with urllib.request.urlopen(req,timeout=20) as r:return r.read(),r.geturl()
class Imgs(HTMLParser):
 def __init__(self):super().__init__();self.images=[]
 def handle_starttag(self,tag,attrs):
  a=dict(attrs)
  if tag=='img':
   src=a.get('src') or a.get('data-src')
   if src:self.images.append((src,' '.join(a.get(k,'') for k in ['alt','class','id'])))
def collect(item):
 name,url=item; key=re.sub('[^a-z0-9]+','_',name.lower()).strip('_');results=[]
 try:
  data,base=get(url);s=data.decode('utf-8','replace');(OUT/(key+'.html')).write_text(s)
  p=Imgs();p.feed(s)
  candidates=[]
  for src,meta in p.images:
   if src.startswith('data:'):continue
   score=(5 if 'logo' in (src+' '+meta).lower() else 0)+(3 if name.lower().split()[0] in meta.lower() else 0)
   if name=='Thunderchild Cultivation LP' and 'thunderchild' in (src+' '+meta).lower():score+=8
   if score>=5:candidates.append((score,urllib.parse.urljoin(base,html.unescape(src)),meta))
  for i,(_,src,meta) in enumerate(sorted(set(candidates),reverse=True)[:3]):
   try:
    b,_=get(src);ext=urllib.parse.urlparse(src).path.rsplit('.',1)[-1].lower();ext=ext if ext in ['svg','png','jpg','jpeg','webp','gif'] else 'bin'
    filename=key+'_'+str(i)+'.'+ext;(OUT/filename).write_bytes(b);results.append({'name':name,'file':filename,'url':src,'website':base,'alt':meta})
   except Exception:pass
  if not results:
   # Inline SVG wordmarks in the masthead, retained for visual review before use.
   svgs=re.findall(r'<svg\b.*?</svg>',s,re.S)
   for i,svg in enumerate(svgs[:2]):
    if len(svg)>1000 and '<path' in svg:
     filename=key+'_inline_'+str(i)+'.svg';(OUT/filename).write_text(svg);results.append({'name':name,'file':filename,'url':base,'website':base,'alt':'inline SVG candidate'})
 except Exception as e:print(name,type(e).__name__,str(e),flush=True)
 print(name,len(results),'candidates',flush=True);return results
all_results=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
 for result in pool.map(collect,SITES.items()):all_results.extend(result)
(OUT/'manifest.json').write_text(json.dumps(all_results,indent=2))
