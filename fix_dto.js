const fs = require('fs');

const servicePath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\data-access\\services\\colaborador.service.ts';
let serviceContent = fs.readFileSync(servicePath, 'utf8');
serviceContent = serviceContent.replace('readonly firstName?: string | null;', 'readonly nombre?: string | null;');
serviceContent = serviceContent.replace('readonly lastName?: string | null;', 'readonly apellido?: string | null;');
fs.writeFileSync(servicePath, serviceContent, 'utf8');

const htmlPath = 'c:\\Users\\emman\\OneDrive\\Desktop\\TPI\\recred-frontend\\src\\app\\features\\perfil-usuario\\perfil-usuario.page.html';
let htmlContent = fs.readFileSync(htmlPath, 'utf8');
htmlContent = htmlContent.replace(/colab\.firstName/g, 'colab.nombre');
htmlContent = htmlContent.replace(/colab\.lastName/g, 'colab.apellido');
fs.writeFileSync(htmlPath, htmlContent, 'utf8');
