// Quick sanity check of the allowlist regex used in inputValidator.ts
const MAX = 256;
const re = new RegExp(`^[\\p{L}\\p{N} .,\\-_:]{0,${MAX}}$`, "u");
const ok = ["Mast ved Lillesand", "Kraftledning Ålesund-Ørsta", ""];
const bad = ["<script>", "Mast\n<b>", "x\" onmouseover=\"", "te%%sting", "a".repeat(MAX + 1)];
let fails = 0;
for (const s of ok) if (!re.test(s)) { fails++; console.log("should accept:", s); }
for (const s of bad) if (re.test(s)) { fails++; console.log("should reject:", s); }
console.log(fails === 0 ? "frontend regex OK" : `${fails} failures`);
process.exit(fails ? 1 : 0);
