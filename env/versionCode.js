function pad(n, width, z) {
  z = z || '0';
  n = n + '';
  return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

var versionCode = process.argv[2];
var each = versionCode.split('.');
var versionCodeNumber = each.reduce((acc, curr) => {
  return acc + pad(parseInt(curr), 3);
}, '');

console.log(versionCodeNumber);