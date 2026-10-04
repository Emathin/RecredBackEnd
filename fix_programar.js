const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredFrontEnd\\\\src\\\\app\\\\features\\\\programar-recarga\\\\programar-recarga.component.ts';

const tsContent = `import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AlumnoContextoService } from '../../core/services/alumno-contexto.service';
import { AlumnosService } from '../../data-access/services/alumnos.service';
import { Alumno } from '../../data-access/models/alumno.model';

@Component({
  selector: 'app-programar-recarga',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './programar-recarga.component.html',
  styleUrls: ['./programar-recarga.component.css']
})
export class ProgramarRecargaComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly contextoService = inject(AlumnoContextoService);
  private readonly alumnosService = inject(AlumnosService);

  readonly cargando = signal(false);
  readonly alumno = signal<Alumno | null>(null);

  ngOnInit() {
    const alumnoId = this.contextoService.alumnoId() || localStorage.getItem('alumnoId');
    if (alumnoId) {
      const al = this.alumnosService.getAlumnoById(alumnoId);
      if (al) {
        this.alumno.set(al);
      } else {
        this.volver();
      }
    } else {
      this.volver();
    }
  }

  volver() {
    this.router.navigate(['/']);
  }

  selectMonto(monto: number) {
    const input = document.getElementById('monto') as HTMLInputElement;
    if (input) input.value = monto.toString();
  }

  onSubmit(event: Event) {
    event.preventDefault();
    const input = document.getElementById('monto') as HTMLInputElement;
    const montoStr = input ? input.value : '0';
    const monto = Number(montoStr);
    if (!monto || monto < 1) return;

    this.cargando.set(true);
    const token = localStorage.getItem('token');
    const alumnoId = this.alumno()?.id;

    fetch('http://localhost:8080/api/v1/payments/schedule-topup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
        body: JSON.stringify({ studentId: alumnoId, amount: monto, frequency: 1, frequencyType: 'months' })
    }).then(res => res.json()).then(data => { 
        if(data.paymentUrl) { window.location.href = data.paymentUrl; } else { this.cargando.set(false); }
    }).catch(err => {
        console.error(err);
        this.cargando.set(false);
    });
  }
}
`;

fs.writeFileSync(file, tsContent);
console.log('Fixed ProgramarRecarga TS');
