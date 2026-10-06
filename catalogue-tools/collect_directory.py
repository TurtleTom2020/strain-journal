import urllib.request,urllib.parse,json,pathlib,concurrent.futures,re
ROOT=pathlib.Path('source-export');BASE='https://thecannabispages.com/wp-json/wp/v2/'
def get(url):
 req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0','Accept':'application/json'})
 with urllib.request.urlopen(req,timeout=35) as r:return r.read(),dict(r.headers)
products=[]
try:
 url=BASE+'at_biz_dir?per_page=100&search=Medicinal%20Cannabis%20Flower&_fields=id,title,content,link,featured_media,meta'
 b,h=get(url);products+=json.loads(b);pages=min(30,int(h.get('X-WP-TotalPages',h.get('x-wp-totalpages',1))))
 for page in range(2,pages+1):
  b,_=get(url+'&page='+str(page));products+=json.loads(b)
 (ROOT/'tcp-products.json').write_text(json.dumps(products));print('directory listings',len(products),flush=True)
except Exception as e:print('products',type(e).__name__,str(e),flush=True)
COMPANIES=['Khiron','Hexacan','Noidecs','Weeco','Avextra','Clearleaf','IPS','Therismos','Althea','Truro','MedCan','SNDL','AgMedica','Avant','Enua','CannFX','Cannada Craft','Common Roots','Cookies','Redecan','Sundaze','BC Green','Green Joy','Kootenay Quantum','Spirit Bear','HighGreens','Kanabo','Muzo','Wellford','Northern Leaf','PhCann','PharmaCann','Dycar','Greenway','Greentone','Schroll Medical','Dalgety','ECS Botanics','SafriCanna','Thunderchild','Decibel']
media=[];out=ROOT/'directory-logos';out.mkdir(exist_ok=True)
def collect(name):
 results=[]
 try:
  b,_=get(BASE+'media?per_page=30&search='+urllib.parse.quote(name));items=json.loads(b)
  (out/(re.sub('[^a-z0-9]','_',name.lower())+'.json')).write_bytes(b)
  for x in items:
   title=x.get('title',{}).get('rendered','');url=x['source_url'];text=title+' '+url+' '+x.get('alt_text','')
   if 'logo' not in text.lower():continue
   try:
    image,_=get(url);filename=str(x['id'])+'.'+url.rsplit('.',1)[-1];(out/filename).write_bytes(image)
    results.append({'name':name,'file':filename,'url':url,'website':x.get('link',url),'title':title})
   except Exception:pass
 except Exception as e:print(name,type(e).__name__,str(e),flush=True)
 print(name,len(results),'logo candidates',flush=True);return results
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
 for result in pool.map(collect,COMPANIES):media.extend(result)
(out/'manifest.json').write_text(json.dumps(media,indent=2))
