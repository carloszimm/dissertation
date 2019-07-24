const R = require('ramda');
const latinSquare = require('latin-square');
const shuffle = require('shuffle-array');

let blocking_vars = ['Board 1', 'Board 2', 'Board 3'];

let treatments  = ['Treatment A', 'Treatment B'];
let treatSampler = latinSquare(treatments);
let treatmentsLatin = [treatSampler(), treatSampler()];
treatmentsLatin.push(treatmentsLatin[0]);


treatmentsLatin.forEach((val, i)=>{
    console.log(`Board ${i + 1}`, val);
});

console.log('\nwhith:');

[
[shuffle(treatments), shuffle(['RxJS', 'Most.js'])],
[shuffle(blocking_vars), shuffle(['Orange Pi PC plus', 'Raspberry Pi 3 B', 'Raspberry Pi 3 B+'])]
].forEach((val) =>{
    R.transpose(val).forEach((transposed)=>{
        console.log(transposed[0] + ' = ' + transposed[1]);
    });
});