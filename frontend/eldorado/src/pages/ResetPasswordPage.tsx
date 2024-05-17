import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card.tsx";
import eldoLogo from "@/assets/eldorado.png";
import {Button} from "@/components/ui/button.tsx";
import {z} from "zod";
import {useToast} from "@/components/ui/use-toast.ts";
import {useForm} from "react-hook-form";
import {zodResolver} from "@hookform/resolvers/zod";
import {api} from "@/api/api.ts";
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from "@/components/ui/form.tsx";
import {Input} from "@/components/ui/input.tsx";
import {useNavigate, useParams} from "react-router-dom";
import {useTranslation} from "react-i18next";


function ResetPasswordPage() {
    const {token} = useParams<{ token: string }>();
    const decodedToken = decodeURIComponent(token!);
    const {toast} = useToast();
    const navigate = useNavigate();
    const {t} = useTranslation();

    const formSchema = z.object({
        password: z.string().min(8, {message: t("resetPasswordPage.passwordTooShort")})
            .max(60, {message: t("resetPasswordPage.passwordTooLong")}),
    })

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            password: "",
        },
    })

    function onSubmit(values: z.infer<typeof formSchema>) {
        // console.table(values)
        api.resetPassword(decodedToken, values.password)
            .then(() => {
                toast({
                    title: t("resetPasswordPage.popUp.resetPasswordOK.title"),
                    description: t("resetPasswordPage.popUp.resetPasswordOK.text"),
                    action: (
                        <div>
                            <Button onClick={() => {
                                navigate('/login', {replace: true});
                            }}>
                                {t("resetPasswordPage.popUp.resetPasswordOK.button")}
                            </Button>
                        </div>
                    ),
                });
            })
            .catch((error) => {
                if (error.response && error.response.data) {
                    const {message, violations} = error.response.data;
                    const violationMessages = violations.map((violation: string | string[]) => t(violation)).join(", ");

                    toast({
                        variant: "destructive",
                        title: t(message),
                        description: violationMessages,
                    });
                } else {
                    toast({
                        variant: "destructive",
                        description: "Error",
                    });
                }
                // console.log(error.response ? error.response.data : error);
            });
    }

    return (
        <div>
            <img src={eldoLogo} alt="Eldorado" className="mx-auto h-auto w-1/2"/>
            <Card className="mx-auto max-w-2xl">
                <CardHeader>
                    <CardTitle>{t("resetPasswordPage.title")}</CardTitle>
                    <CardDescription>{t("resetPasswordPage.info")}</CardDescription>
                </CardHeader>
                <CardContent>
                    <Form {...form}>
                        <form onSubmit={form.handleSubmit(onSubmit)}>
                            <div className="flex flex-col space-y-4">
                                <div>
                                    <FormField
                                        control={form.control}
                                        name="password"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel
                                                    className="text-black">{t("resetPasswordPage.password")}</FormLabel>
                                                <FormControl>
                                                    <Input type="password" {...field}/>
                                                </FormControl>
                                                <FormMessage/>
                                            </FormItem>
                                        )}
                                    />
                                </div>
                                <Button type="submit">{t("resetPasswordPage.button")}</Button>
                            </div>
                        </form>
                    </Form>
                </CardContent>
            </Card>
        </div>
    );
}

export default ResetPasswordPage;