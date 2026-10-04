const fs = require('fs');
const file = 'C:\\\\Users\\\\emman\\\\OneDrive\\\\Desktop\\\\Final TPI\\\\RecredFrontEnd\\\\src\\\\app\\\\features\\\\programar-recarga\\\\programar-recarga.component.ts';

const tsContent = `import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
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
  private readonly http = inject(HttpClient);
  private readonly apiBase = environment.apiUrl;

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

  async onSubmit(event: Event) {
    event.preventDefault();
    const input = document.getElementById('monto') as HTMLInputElement;
    const montoStr = input ? input.value : '0';
    const monto = Number(montoStr);
    if (!monto || monto < 1) return;

    this.cargando.set(true);
    const alumnoId = this.alumno()?.id;

    try {
      const body = { studentId: alumnoId, amount: monto, frequency: 1, frequencyType: 'months' };
      const response = await firstValueFrom(
        this.http.post<any>(\`\${this.apiBase}/payments/schedule-topup\`, body)
      );
      if (response.paymentUrl) {
        window.location.href = response.paymentUrl;
      } else {
        this.cargando.set(false);
      }
    } catch (err) {
      console.error(err);
      this.cargando.set(false);
    }
  }
}
`;

fs.writeFileSync(file, tsContent);
console.log('Fixed auth issue via HttpClient');
