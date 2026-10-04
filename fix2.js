const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredFrontEnd\\\\src\\\\app\\\\features\\\\home-tutor\\\\components\\\\alumno-card\\\\alumno-card.component.ts';
let c = fs.readFileSync(file, 'utf8');

c = c.replace(/programarRecarga\(\) \{[\s\S]*$/, "programarRecarga() { this.navegar('/programar-recarga'); }\n}\n");

fs.writeFileSync(file, c);
