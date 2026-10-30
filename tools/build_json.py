import json,re,html,os,glob
M=['enero','febrero','marzo','abril','mayo','junio','julio','agosto','septiembre','octubre','noviembre','diciembre']
WB='https://web.archive.org/web/'
def larga(f):
    y,m,d=f.split('-'); return '%d de %s de %s'%(int(d),M[int(m)-1],y)
def yt(i,t,info): return {'t':t,'u':'https://www.youtube.com/watch?v='+i,'info':info}
def rd(p,enc='utf-8'):
    b=open(p,'rb').read()
    try: return b.decode('utf-8')
    except: return b.decode('latin-1')
def meta(s,n):
    m=re.search(r'<meta[^>]+(?:name|property)="'+n+r'"[^>]+content="([^"]*)"',s) or re.search(r'<meta[^>]+content="([^"]*)"[^>]+(?:name|property)="'+n+'"',s)
    return html.unescape(m.group(1)).strip() if m else ''
# ---- enlaces públicos (verificados) ----
L={
 '2013-02-07':[yt('CGZaNQhPR04','Crepes de pollo, lechuga y nueces – Karlos Arguiñano (Hogarmania)','YouTube · Hogarmania · 4:40 · receta'),
               yt('Lp2A1YgUokA','Crepes salados de pollo, lechuga y nueces (Cocina Abierta)','YouTube · Cocina Abierta · 19:17 · versión reciente'),
               yt('3QsXeHHREeg','Receta: Crepés de pollo, lechuga y nueces','YouTube · Canal 3 Guatemala · 8:02')],
}
AC={
 '2011-08-29':[yt('k3rq5fGk6vM','Ahora Caigo 29-08-2011','YouTube · Alfonso Naranjo · 7:02'),yt('olNcHLNi0T0','AHORA CAIGO 29-08-2011 parte 2','YouTube · Alfonso Naranjo · 8:48')],
 '2011-09-01':[yt('GkyEXlPZhc8','Daniel Astillero AHORA CAIGO 01/09/2011 (I)','YouTube · 9:07')],
 '2011-09-05':[yt('vmtl0QxzJQo','Ahora Caigo 05/09/11 (parte 1)','YouTube · lorendesantia · 15:01'),yt('3o0gg0cYxhQ','Ahora Caigo 05/09/11 (parte 2)','YouTube · lorendesantia · 15:03')],
 '2011-09-15':[yt('qbHS0jbDlog','Ahora Caigo 15-Sept-2011','YouTube · hmh88 · 14:57')],
 '2011-09-20':[yt('ORZ679TdTWU','Vito en Ahora Caigo! 20 Sep 2011 (programa entero)','YouTube · Vito González Calvo · 1:05:26')],
 '2011-10-13':[yt('JiAPTobzBwg','Ahora Caigo 13/10/2011 Caídas','YouTube · 1:21'),yt('X00ciXW7GIo','Ahora Caigo 13/10/2011 Parte 3-3 (Caídas)','YouTube · 1:21')],
 '2011-10-17':[yt('_-tFCesLMEg','ahora caigo Manuel 17/10/2011','YouTube · parejovali · 8:13')],
 '2011-10-25':[yt('zABt2vXsjoc','AHORA CAIGO 25 octubre 2011 Arantxa vs Juan','YouTube · 9:48')],
 '2011-12-07':[{'t':'ALEX GADEA AHORA CAIGO CONCURSA 07-12-2011 (1)','u':'https://www.dailymotion.com/video/xmtytj','info':'Dailymotion · 31:44'}],
 '2012-08-06':[yt('kee0PdQj8Qg','Ahora caigo - 6 de agosto 2012 Parte 1','YouTube · Hugo Muñoz · 14:31')],
 '2014-06-09':[yt('S4pobyRM36o','Ahora Caigo 9 junio 2014 (programa entero)','YouTube · Miguel Poveda · 1:03:01')],
 '2021-08-01':[yt('D8rAfCgb5E8','Arturo Valls emociona con un vídeo resumen de los diez años','YouTube · Antena 3 · 2:46'),
               yt('EZoRX_uA8Po','El emotivo mensaje de Arturo Valls tras el final','YouTube · Antena 3 · 0:27'),
               yt('TJEyEh2hhGM','Promo especial ¡Ahora Caigo! despedida 10 años','YouTube · Susana A. · 0:25')],
}
PUB=[]
seen=set()
for d,ls in list(L.items())+list(AC.items()):
    for x in ls:
        if x['u'] not in seen: seen.add(x['u']); PUB.append(dict(x,t=x['t']+' ('+d+')' if d not in x['t'] else x['t']))
extra=[yt('7jjOiyRpfZI','Cabecera Karlos Arguiñano en tu cocina 2013-2014','YouTube · Hogarmania · 0:24'),
 yt('GMZzCUWA3aY','Cabecera Karlos Arguiñano 2012-2013','YouTube · Hogarmania · 0:19'),
 yt('LLlfPd2mxd0','Canción del programa Karlos Arguiñano en tu cocina 2011','YouTube · Hogarmania · 0:22'),
 yt('WRwycWYFhsI','Cabecera de Karlos Arguiñano en tu Cocina (Antena 3)','YouTube · Antena 3 · 0:24'),
 yt('vf8Vfma8UAs','Karlos Arguiñano vuelve el 17 de septiembre con nuevas recetas','YouTube · Antena 3 · 0:21'),
 yt('XgElo0HxEHc','Mónica Naranjo en Karlos Arguiñano en tu cocina (17 feb 2011, parte 1)','YouTube · 2:57'),
 {'t':'Karlos Arguiñano en tu cocina – cabecera (2011)','u':'https://www.dailymotion.com/video/xh40u4','info':'Dailymotion · 0:17'},
 yt('3joe_s84xtE','Cabecera Ahora Caigo – Antena 3 2011','YouTube · CanalMira2 · 1:06'),
 yt('JyPY-sbPoKc','Promo histórica ¡Ahora Caigo! (2011) estreno en Antena 3','YouTube · 0:20'),
 yt('QOm2KdeR2Kk','Primer anuncio de Ahora Caigo con Arturo Valls','YouTube · 0:23'),
 yt('qVpst6mohl8','ahora caigo debut','YouTube · TaniaMarifly · 11:09'),
 {'t':'¡Ahora caigo! – 18 de octubre de 2014 (programa entero)','u':'https://archive.org/details/ahora-caigo-2014-10-18','info':'archive.org · 1:00:00'},
 {'t':'Karlos Arguiñano en tu cocina – temporadas 1997-2000 (Telefe, Argentina)','u':'https://archive.org/details/karlos-arguinano-en-tu-cocina-cuarta-temporada-2000-1080p','info':'archive.org · programas enteros'}]
PUB+=extra
fichas={'arg':[],'ac':[],'hu':[]}
# ---- Hogarutil ----
hu={}
for line in open('hu_list.txt'):
    k,ts,u=line.split(); f='hu/%s.html'%k
    fecha='%s-%s-%s'%(k[:4],k[4:6],k[6:8]); rec=[]; tit=''
    if os.path.exists(f):
        s=rd(f); m=re.search(r'<title>(.*?)</title>',s,re.S)
        if m:
            tit=html.unescape(m.group(1)).strip(); r=re.sub(r'\s+de Karlos Argui.ano en tu cocina\s*$','',tit)
            if r and r!=tit: rec=[x.strip() for x in re.split(r'\s+y\s+(?=[A-ZÁÉÍÓÚÑ])|, (?=[A-ZÁÉÍÓÚÑ])',r) if x.strip()]
    hu[fecha]=(ts,u,rec)
    f2={'titulo':(' · '.join(rec) if rec else 'Programa del '+larga(fecha)),'fecha':fecha,
        'descripcion':'Emisión del programa Karlos Arguiñano en tu cocina en Hogarutil TV.' if rec else 'Página del programa guardada en Hogarutil TV (Wayback Machine).',
        'recetas':rec,'imagen':'','partes':0,'paginas':[{'t':'Hogarutil TV','u':WB+ts+'/'+u}]}
    if fecha in L: f2['enlaces']=L[fecha]
    fichas['hu'].append(f2)
# ---- Antena 3 Arguiñano ----
for line in open('a3_list.txt'):
    k,ts,u=line.split()
    m=re.search(r'/(\d{4})-(\w+)-(\d+)-\d+\.html',u)
    if not m or m.group(2) not in M: continue
    fecha='%s-%02d-%02d'%(m.group(1),M.index(m.group(2))+1,int(m.group(3)))
    f='a3/%s.html'%k; img=''; partes=0; cap=''
    if os.path.exists(f):
        s=rd(f); p=re.search(r"player_capitulo\.poster\s*=\s*'([^']+)'",s); n=re.search(r"player_capitulo\.parts\s*=\s*'(\d+)'",s)
        c=re.search(r'<li>(Cap[ií]tulo[^<]*)</li>',s)
        li=re.search(r'<img title="imagen capitulo" src="([^"]+)"',s)
        cands=[]
        if li: cands.append(WB+ts[:6]+'id_/http://www.antena3.com'+li.group(1))
        if p: cands.append(WB+ts[:6]+'id_/http://www.antena3.com'+p.group(1))
        img='|'.join(cands)
        if n: partes=int(n.group(1))
        if c: cap=html.unescape(c.group(1))
    h=hu.get(fecha); rec=h[2] if h else []
    pags=[{'t':'Antena 3 (vídeos)','u':WB+ts+'/'+u}]
    if h: pags.append({'t':'Hogarutil TV','u':WB+h[0]+'/'+h[1]})
    d='Programa completo de Karlos Arguiñano en tu cocina emitido en Antena 3.'+(' '+cap+'.' if cap else '')
    f2={'titulo':(' · '.join(rec) if rec else 'Programa del '+larga(fecha)),'fecha':fecha,'descripcion':d,'recetas':rec,'imagen':img,'partes':partes,'paginas':pags}
    if fecha in L: f2['enlaces']=L[fecha]
    fichas['arg'].append(f2)
# ---- Ahora Caigo ----
fichas['ac'].append({'titulo':'Ahora Caigo – Programa 2166 Especial 10 años','fecha':'2021-08-01',
  'descripcion':'Ahora Caigo - Programa 2166 Especial 10 años (01-08-21). En ATRESPlayer. Último programa con Arturo Valls tras diez años.',
  'recetas':[],'imagen':'https://imagenes.atresplayer.com/atp/clipping/cmsimages01/2021/07/30/DA4C8E66-5E66-4E44-AD23-47867CA56C5D//720x540.jpg','partes':0,
  'paginas':[{'t':'Atresplayer (guardada)','u':'https://web.archive.org/web/20220517210544/https://www.atresplayer.com/antena3/programas/ahora-caigo/temporada-1/01-08-21-especial-10-anos_60ee0bdd6584a872911240a6/'},
             {'t':'Atresplayer (página oficial)','u':'https://www.atresplayer.com/antena3/programas/ahora-caigo/temporada-1/01-08-21-especial-10-anos_60ee0bdd6584a872911240a6/'}],
  'enlaces':AC['2021-08-01']})
fichas['ac'].append({'titulo':'Ahora Caigo – programa de agosto de 2011 (6 partes)','fecha':'2011-08-16',
  'descripcion':'La página guardada dice "Capítulo 1 - Temporada 2011, emitido el 16 de enero de 2011", pero su vídeo (chapterxml) es del 16 de agosto de 2011, con 6 partes MP4 (no guardadas).',
  'recetas':[],'imagen':WB+'20150610224534id_/http://www.antena3.com/clipping/2011/07/05/00405/30.jpg|'+WB+'20130207162320id_/http://www.antena3.com/clipping/2011/07/05/00405/7.jpg','partes':6,
  'paginas':[{'t':'Antena 3 (vídeos)','u':WB+'20110903171536/http://www.antena3.com/videos/ahora-caigo/2011-enero-16.html'},
             {'t':'XML del capítulo','u':WB+'20250220200326/https://www.antena3.com/chapterxml/6/1003563/2011/08/16/00005.xml'}]})
for line in open('ac_list.txt'):
    k,ts,u=line.split(); fecha='%s-%s-%s'%(k[:4],k[4:6],k[6:8]); f='ac/%s.html'%k
    if not os.path.exists(f): continue
    s=rd(f); t=meta(s,'og:title'); d=meta(s,'og:description'); img=meta(s,'og:image')
    if not t: continue
    if img: img=re.sub(r'width=\d+&height=\d+','width=480&height=270',img)
    f2={'titulo':t,'fecha':fecha,'descripcion':d,'recetas':[],'imagen':img,'partes':0,
        'paginas':[{'t':'Antena 3 (guardada)','u':WB+ts+'/'+u},{'t':'Antena 3 (página actual)','u':u}]}
    if fecha in AC: f2['enlaces']=AC[fecha]
    fichas['ac'].append(f2)
for k in fichas: fichas[k].sort(key=lambda x:x['fecha'])
def yrs(l): return '%s–%s'%(l[0]['fecha'][:4],l[-1]['fecha'][:4]) if l else ''
out={'intro':'¡Bienvenido al museo, Iker! Fichas de programas reales guardados en la Wayback Machine 😊','publicos':PUB,'programas':[
 {'id':'arg','nombre':'Karlos Arguiñano en tu cocina','emoji':'👨‍🍳','color':'FFFF8A3D','descripcion':'Antena 3. Programas completos con su portada, fecha y recetas.','años':yrs(fichas['arg']),'fichas':fichas['arg']},
 {'id':'ac','nombre':'Ahora Caigo','emoji':'🕳️','color':'FF1E88E5','descripcion':'Antena 3 con Arturo Valls. Momentos de los primeros años y el Especial 10 años.','años':yrs(fichas['ac']),'fichas':fichas['ac']},
 {'id':'hu','nombre':'Hogarutil TV (Arguiñano)','emoji':'🍳','color':'FF43A047','descripcion':'Las emisiones de Arguiñano en Hogarutil TV, con el nombre de cada receta.','años':yrs(fichas['hu']),'fichas':fichas['hu']}]}
json.dump(out,open('museo.json','w'),ensure_ascii=False,separators=(',',':'))
print({k:len(v) for k,v in fichas.items()}, 'pub',len(PUB), os.path.getsize('museo.json'))
print(sum(1 for x in fichas['arg'] if x['imagen']),'arg con imagen;',sum(1 for x in fichas['arg'] if x['recetas']),'arg con recetas;',sum(1 for x in fichas['hu'] if x['recetas']),'hu con recetas')
