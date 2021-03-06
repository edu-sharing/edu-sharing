import { Directive, Input } from '@angular/core';

/**
 * Add an arbitrary property to any object.
 */
@Directive({
    selector: '[appRegisterCustomProperty]',
})
export class RegisterCustomPropertyDirective {
    @Input('appRegisterCustomProperty') set property(p: { key: string; value: any; object: any }) {
        p.object[p.key] = p.value;
    }

    constructor() {}
}
