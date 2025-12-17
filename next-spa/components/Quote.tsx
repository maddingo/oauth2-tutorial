'use client';

import { useAuth } from '@/context/AuthContext';
import {getQuote} from "@/lib/quote-api";

export default function Quote() {
    const { accessToken } = useAuth();

    //const quote = {quote: "The only way to do great work is to love what you do.", author: "Artur Schramm"};
    const quote = await getQuote(accessToken!);
    return (<div>
                <div className="quote">{quote.quote}</div>
                <div className="author">{quote.author}</div>
            </div>
    );
}
