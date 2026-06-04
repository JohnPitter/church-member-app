// Semeia os layouts Server-Driven UI no Firestore.
// Cada arquivo .json vira o campo `json` (string) do doc correspondente.
//
// Uso (a partir desta pasta ou da raiz do repo web, onde há node_modules com firebase-admin):
//   GOOGLE_APPLICATION_CREDENTIALS=/caminho/serviceAccountKey.json node mobile/sdui-layouts/seed.js
// Se a env não for definida, tenta ../../serviceAccountKey.json (raiz do repo web).
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const keyPath = process.env.GOOGLE_APPLICATION_CREDENTIALS
  || path.resolve(__dirname, '../../serviceAccountKey.json');
admin.initializeApp({ credential: admin.credential.cert(require(keyPath)) });
const db = admin.firestore();

// arquivo -> [coleção, id]
const DOCS = {
  'home.json': ['appScreens', 'home'],
  'events.json': ['appScreens', 'events'],
  'devotionals.json': ['appScreens', 'devotionals'],
  'blog.json': ['appScreens', 'blog'],
  'projects.json': ['appScreens', 'projects'],
  'leadership.json': ['appScreens', 'leadership'],
  'lives.json': ['appScreens', 'lives'],
  'forum.json': ['appScreens', 'forum'],
  'appConfig.json': ['appConfig', 'main'],
};

(async () => {
  for (const [file, [coll, id]] of Object.entries(DOCS)) {
    const raw = fs.readFileSync(path.join(__dirname, file), 'utf8');
    JSON.parse(raw); // valida antes de gravar
    await db.collection(coll).doc(id).set({ json: raw });
    console.log(`set ${coll}/${id} (${raw.length} chars)`);
  }
  console.log('SEED OK');
  process.exit(0);
})().catch((e) => { console.error('SEED FAIL', e); process.exit(1); });
