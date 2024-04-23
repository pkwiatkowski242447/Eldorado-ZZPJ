// import {ApiResponseType} from "../types/ApiResponseType";
import {apiWithConfig} from "./api.config";
// import {AccessToken} from "../types/TokenPayload";

export const api = {
    logIn: (login: string, password: string) => {
        return apiWithConfig.post('/auth/login', { login, password })
    },
    // logOut: (): ApiResponseType<any> => {
    //     return apiWithConfig.post('/auth/logout')
    // },
    // register: (login:string, password:string, firstName:string, lastName:string) => {
    //     return apiWithConfig.post('/auth/register',{login,password, firstName, lastName})
    // }
}