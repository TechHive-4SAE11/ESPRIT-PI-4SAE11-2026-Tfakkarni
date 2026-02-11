import { Directive, signal } from '@angular/core';

let nextUniqueId = 0;

@Directive({
  selector: '[zardId]',
  exportAs: 'zardId',
  standalone: true,
})
export class ZardIdDirective {
  readonly id = signal(`zard-${nextUniqueId++}`);

  getId(): string {
    return this.id();
  }
}
